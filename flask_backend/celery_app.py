from celery import Celery
from flask import Flask
import os

def make_celery(app: Flask) -> Celery:
    """Create and configure Celery instance"""
    celery = Celery(
        app.import_name,
        backend=app.config['CELERY_RESULT_BACKEND'],
        broker=app.config['CELERY_BROKER_URL']
    )
    
    # Update configuration from Flask app
    celery.conf.update(
        task_serializer='json',
        accept_content=['json'],
        result_serializer='json',
        timezone='UTC',
        enable_utc=True,
        task_track_started=True,
        task_time_limit=30 * 60,  # 30 minutes
        task_soft_time_limit=25 * 60,  # 25 minutes
        worker_prefetch_multiplier=1,
        worker_max_tasks_per_child=1000,
    )
    
    # Create task context
    class ContextTask(celery.Task):
        """Make celery tasks work with Flask app context"""
        def __call__(self, *args, **kwargs):
            with app.app_context():
                return self.run(*args, **kwargs)
    
    celery.Task = ContextTask
    return celery

# Standalone Celery app for worker processes
def create_celery_app():
    """Create Celery app for worker processes"""
    from config import Config
    
    app = Flask(__name__)
    app.config.from_object(Config)
    
    celery = make_celery(app)
    
    # Import tasks to register them
    from tasks import (
        process_sensor_data,
        generate_ai_recommendations,
        check_automation_rules,
        send_notification,
        cleanup_old_data
    )
    
    return celery

# For worker processes
celery_app = create_celery_app()

if __name__ == '__main__':
    celery_app.start()