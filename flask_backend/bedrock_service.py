import boto3
import json
from datetime import datetime, timedelta
from typing import Optional, Dict, Any
from sqlalchemy import func, desc

from models import db, SensorData, Recommendation, Room, Device
from config import Config

class BedrockService:
    """
    Service for integrating with AWS Bedrock for AI-powered recommendations
    """
    
    def __init__(self):
        self.bedrock_client = boto3.client(
            'bedrock-runtime',
            region_name=Config.AWS_REGION,
            aws_access_key_id=Config.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=Config.AWS_SECRET_ACCESS_KEY
        )
        self.model_id = Config.BEDROCK_MODEL_ID
    
    def analyze_room(self, room_id: str) -> Optional[Recommendation]:
        """
        Analyze room conditions and generate AI recommendations
        """
        try:
            # Get room information
            room = Room.query.get(room_id)
            if not room:
                return None
            
            # Get recent sensor data (last 24 hours)
            end_time = datetime.utcnow()
            start_time = end_time - timedelta(hours=24)
            
            sensor_stats = self._get_sensor_statistics(room_id, start_time, end_time)
            if not sensor_stats:
                return None
            
            # Create analysis prompt
            prompt = self._create_analysis_prompt(room, sensor_stats)
            
            # Call Bedrock for analysis
            analysis_result = self._call_bedrock(prompt)
            if not analysis_result:
                return None
            
            # Create recommendation record
            recommendation = Recommendation(
                room_id=room_id,
                type='analysis',
                content=analysis_result,
                confidence_score=analysis_result.get('confidence', 0.8),
                created_at=datetime.utcnow()
            )
            
            db.session.add(recommendation)
            db.session.commit()
            
            return recommendation
            
        except Exception as e:
            print(f"Error in room analysis: {str(e)}")
            return None
    
    def _get_sensor_statistics(self, room_id: str, start_time: datetime, end_time: datetime) -> Optional[Dict[str, Any]]:
        """
        Get aggregated sensor statistics for the specified time period
        """
        try:
            # Get devices in the room
            devices = Device.query.filter_by(room_id=room_id).all()
            device_ids = [device.id for device in devices]
            
            if not device_ids:
                return None
            
            # Query sensor data
            sensor_data = db.session.query(
                SensorData.sensor_type,
                func.avg(SensorData.value).label('avg_value'),
                func.min(SensorData.value).label('min_value'),
                func.max(SensorData.value).label('max_value'),
                func.count(SensorData.id).label('count')
            ).filter(
                SensorData.device_id.in_(device_ids),
                SensorData.timestamp >= start_time,
                SensorData.timestamp <= end_time
            ).group_by(SensorData.sensor_type).all()
            
            if not sensor_data:
                return None
            
            # Organize statistics by sensor type
            stats = {
                'temperature': {'avg': None, 'min': None, 'max': None, 'count': 0},
                'humidity': {'avg': None, 'min': None, 'max': None, 'count': 0},
                'co2': {'avg': None, 'min': None, 'max': None, 'count': 0},
                'light': {'avg': None, 'min': None, 'max': None, 'count': 0},
                'ph': {'avg': None, 'min': None, 'max': None, 'count': 0},
                'moisture': {'avg': None, 'min': None, 'max': None, 'count': 0}
            }
            
            total_count = 0
            for row in sensor_data:
                sensor_type = row.sensor_type.lower()
                if sensor_type in stats:
                    stats[sensor_type] = {
                        'avg': float(row.avg_value),
                        'min': float(row.min_value),
                        'max': float(row.max_value),
                        'count': row.count
                    }
                    total_count += row.count
            
            return {
                'stats': stats,
                'sample_count': total_count,
                'time_range': {
                    'start': start_time.isoformat(),
                    'end': end_time.isoformat()
                }
            }
            
        except Exception as e:
            print(f"Error getting sensor statistics: {str(e)}")
            return None
    
    def _create_analysis_prompt(self, room: Room, sensor_stats: Dict[str, Any]) -> str:
        """
        Create a prompt for AI analysis based on room and sensor data
        """
        # Optimal mushroom growing conditions
        optimal_conditions = {
            'temperature': {'min': 18, 'max': 24, 'unit': '°C'},
            'humidity': {'min': 80, 'max': 95, 'unit': '%'},
            'co2': {'min': 800, 'max': 1200, 'unit': 'ppm'},
            'light': {'min': 200, 'max': 500, 'unit': 'lux'},
            'ph': {'min': 6.0, 'max': 7.5, 'unit': ''},
            'moisture': {'min': 70, 'max': 85, 'unit': '%'}
        }
        
        prompt = f"""
You are an expert mushroom cultivation advisor. Analyze the following room conditions and provide recommendations.

Room Information:
- Name: {room.name}
- Type: {room.room_type}
- Description: {room.description or 'No description'}

Optimal Mushroom Growing Conditions:
- Temperature: {optimal_conditions['temperature']['min']}-{optimal_conditions['temperature']['max']}°C
- Humidity: {optimal_conditions['humidity']['min']}-{optimal_conditions['humidity']['max']}%
- CO2: {optimal_conditions['co2']['min']}-{optimal_conditions['co2']['max']} ppm
- Light: {optimal_conditions['light']['min']}-{optimal_conditions['light']['max']} lux
- pH: {optimal_conditions['ph']['min']}-{optimal_conditions['ph']['max']}
- Moisture: {optimal_conditions['moisture']['min']}-{optimal_conditions['moisture']['max']}%

Actual Conditions (Last 24 hours, {sensor_stats['sample_count']} data points):
"""
        
        # Add temperature analysis
        if sensor_stats['stats']['temperature']['avg'] is not None:
            prompt += f"\nTemperature:\n- Average: {sensor_stats['stats']['temperature']['avg']:.1f}°C"
            prompt += f"\n- Range: {sensor_stats['stats']['temperature']['min']:.1f}°C - {sensor_stats['stats']['temperature']['max']:.1f}°C"
        
        # Add humidity analysis
        if sensor_stats['stats']['humidity']['avg'] is not None:
            prompt += f"\nHumidity:\n- Average: {sensor_stats['stats']['humidity']['avg']:.1f}%"
            prompt += f"\n- Range: {sensor_stats['stats']['humidity']['min']:.1f}% - {sensor_stats['stats']['humidity']['max']:.1f}%"
        
        # Add CO2 analysis
        if sensor_stats['stats']['co2']['avg'] is not None:
            prompt += f"\nCO2:\n- Average: {sensor_stats['stats']['co2']['avg']:.0f} ppm"
            prompt += f"\n- Range: {sensor_stats['stats']['co2']['min']:.0f} - {sensor_stats['stats']['co2']['max']:.0f} ppm"
        
        # Add light analysis
        if sensor_stats['stats']['light']['avg'] is not None:
            prompt += f"\nLight:\n- Average: {sensor_stats['stats']['light']['avg']:.0f} lux"
            prompt += f"\n- Range: {sensor_stats['stats']['light']['min']:.0f} - {sensor_stats['stats']['light']['max']:.0f} lux"
        
        # Add pH analysis
        if sensor_stats['stats']['ph']['avg'] is not None:
            prompt += f"\npH:\n- Average: {sensor_stats['stats']['ph']['avg']:.1f}"
            prompt += f"\n- Range: {sensor_stats['stats']['ph']['min']:.1f} - {sensor_stats['stats']['ph']['max']:.1f}"
        
        # Add moisture analysis
        if sensor_stats['stats']['moisture']['avg'] is not None:
            prompt += f"\nMoisture:\n- Average: {sensor_stats['stats']['moisture']['avg']:.1f}%"
            prompt += f"\n- Range: {sensor_stats['stats']['moisture']['min']:.1f}% - {sensor_stats['stats']['moisture']['max']:.1f}%"
        
        prompt += """

Please provide a detailed analysis and recommendations in JSON format with the following structure:
{
  "overall_status": "excellent|good|fair|poor",
  "confidence": 0.0-1.0,
  "issues": [
    {
      "parameter": "temperature|humidity|co2|light|ph|moisture",
      "severity": "critical|high|medium|low",
      "description": "Description of the issue",
      "recommendation": "Specific action to take"
    }
  ],
  "recommendations": [
    {
      "priority": "high|medium|low",
      "action": "Specific recommendation",
      "expected_impact": "Expected outcome"
    }
  ],
  "summary": "Brief overall assessment and next steps"
}
"""
        
        return prompt
    
    def _call_bedrock(self, prompt: str) -> Optional[Dict[str, Any]]:
        """
        Call AWS Bedrock with the given prompt
        """
        try:
            body = {
                "anthropic_version": "bedrock-2023-05-31",
                "max_tokens": 1000,
                "messages": [
                    {
                        "role": "user",
                        "content": prompt
                    }
                ]
            }
            
            response = self.bedrock_client.invoke_model(
                modelId=self.model_id,
                body=json.dumps(body)
            )
            
            response_body = json.loads(response['body'].read())
            
            # Extract the content from Claude's response
            if 'content' in response_body and len(response_body['content']) > 0:
                content = response_body['content'][0]['text']
                
                # Try to parse JSON from the response
                try:
                    # Look for JSON in the response
                    start_idx = content.find('{')
                    end_idx = content.rfind('}') + 1
                    
                    if start_idx != -1 and end_idx != -1:
                        json_str = content[start_idx:end_idx]
                        return json.loads(json_str)
                    else:
                        # If no JSON found, create a basic response
                        return {
                            "overall_status": "unknown",
                            "confidence": 0.5,
                            "issues": [],
                            "recommendations": [
                                {
                                    "priority": "medium",
                                    "action": "Review sensor data and environmental conditions",
                                    "expected_impact": "Better understanding of room conditions"
                                }
                            ],
                            "summary": content[:200] + "..." if len(content) > 200 else content
                        }
                except json.JSONDecodeError:
                    return {
                        "overall_status": "unknown",
                        "confidence": 0.5,
                        "issues": [],
                        "recommendations": [],
                        "summary": "Unable to parse AI response"
                    }
            
            return None
            
        except Exception as e:
            print(f"Error calling Bedrock: {str(e)}")
            return None
    
    def analyze_yield_prediction(self, room_id: str, days_ahead: int = 7) -> Optional[Dict[str, Any]]:
        """
        Predict mushroom yield based on current conditions
        """
        try:
            room = Room.query.get(room_id)
            if not room:
                return None
            
            # Get historical data for the last 30 days
            end_time = datetime.utcnow()
            start_time = end_time - timedelta(days=30)
            
            sensor_stats = self._get_sensor_statistics(room_id, start_time, end_time)
            if not sensor_stats:
                return None
            
            prompt = f"""
As a mushroom cultivation expert, predict the yield for the next {days_ahead} days based on the following conditions:

Room: {room.name} ({room.room_type})
Historical Data (Last 30 days):
"""
            
            # Add sensor data to prompt
            for sensor_type, data in sensor_stats['stats'].items():
                if data['avg'] is not None:
                    prompt += f"\n{sensor_type.title()}: Avg {data['avg']:.1f}, Range {data['min']:.1f}-{data['max']:.1f}"
            
            prompt += f"""

Provide a yield prediction in JSON format:
{{
  "predicted_yield_kg": 0.0,
  "confidence": 0.0-1.0,
  "factors": [
    {{
      "factor": "temperature|humidity|etc",
      "impact": "positive|negative|neutral",
      "description": "How this factor affects yield"
    }}
  ],
  "recommendations": ["List of actions to optimize yield"],
  "timeline": "Expected harvest timeline"
}}
"""
            
            result = self._call_bedrock(prompt)
            return result
            
        except Exception as e:
            print(f"Error in yield prediction: {str(e)}")
            return None
    
    def generate_automation_suggestions(self, room_id: str) -> Optional[Dict[str, Any]]:
        """
        Generate automation rule suggestions based on room conditions
        """
        try:
            room = Room.query.get(room_id)
            if not room:
                return None
            
            # Get recent sensor data
            end_time = datetime.utcnow()
            start_time = end_time - timedelta(hours=48)
            
            sensor_stats = self._get_sensor_statistics(room_id, start_time, end_time)
            if not sensor_stats:
                return None
            
            prompt = f"""
As a mushroom cultivation automation expert, suggest automation rules for optimal growing conditions.

Room: {room.name}
Current Conditions (Last 48 hours):
"""
            
            # Add sensor data
            for sensor_type, data in sensor_stats['stats'].items():
                if data['avg'] is not None:
                    prompt += f"\n{sensor_type.title()}: {data['avg']:.1f} (Range: {data['min']:.1f}-{data['max']:.1f})"
            
            prompt += """

Suggest automation rules in JSON format:
{
  "rules": [
    {
      "name": "Rule name",
      "trigger": {
        "sensor_type": "temperature|humidity|etc",
        "condition": ">",
        "value": 0.0
      },
      "action": {
        "device_type": "fan|heater|humidifier|etc",
        "command": "on|off|set_value",
        "value": "optional value"
      },
      "priority": "high|medium|low",
      "description": "Why this rule is needed"
    }
  ],
  "summary": "Overall automation strategy"
}
"""
            
            result = self._call_bedrock(prompt)
            return result
            
        except Exception as e:
            print(f"Error generating automation suggestions: {str(e)}")
            return None

# Global instance
bedrock_service = BedrockService()