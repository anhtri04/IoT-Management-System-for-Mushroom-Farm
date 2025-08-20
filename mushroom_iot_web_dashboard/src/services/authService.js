import {
  CognitoUserPool,
  CognitoUser,
  AuthenticationDetails,
  CognitoUserAttribute
} from 'amazon-cognito-identity-js';

// AWS Cognito configuration
const poolData = {
  UserPoolId: import.meta.env.VITE_AWS_USER_POOL_ID || 'ap-southeast-1_sNnppD3OU',
  ClientId: import.meta.env.VITE_AWS_USER_POOL_CLIENT_ID || '3vjk4opq2viuam695ft3fabcn0'
};

const userPool = new CognitoUserPool(poolData);

class AuthService {
  // Get current authenticated user
  getCurrentUser() {
    return new Promise((resolve, reject) => {
      // Development mode bypass
      if (import.meta.env.VITE_AUTH_MODE === 'development') {
        resolve({
          username: 'dev-user',
          email: 'dev@example.com',
          attributes: {
            email: 'dev@example.com',
            name: 'Development User'
          }
        });
        return;
      }

      const cognitoUser = userPool.getCurrentUser();
      
      if (!cognitoUser) {
        reject(new Error('No current user'));
        return;
      }

      cognitoUser.getSession((err, session) => {
        if (err) {
          reject(err);
          return;
        }

        if (!session.isValid()) {
          reject(new Error('Session is not valid'));
          return;
        }

        cognitoUser.getUserAttributes((err, attributes) => {
          if (err) {
            reject(err);
            return;
          }

          const userAttributes = {};
          attributes.forEach(attr => {
            userAttributes[attr.getName()] = attr.getValue();
          });

          resolve({
            username: cognitoUser.getUsername(),
            session,
            attributes: userAttributes
          });
        });
      });
    });
  }

  // Sign in user
  signIn(email, password) {
    return new Promise((resolve, reject) => {
      // Development mode bypass
      if (import.meta.env.VITE_AUTH_MODE === 'development') {
        setTimeout(() => {
          resolve({
            user: {
              username: 'dev-user',
              email: email,
              attributes: {
                email: email,
                name: 'Development User'
              }
            },
            session: { isValid: () => true }
          });
        }, 1000); // Simulate network delay
        return;
      }

      const authenticationDetails = new AuthenticationDetails({
        Username: email,
        Password: password
      });

      const cognitoUser = new CognitoUser({
        Username: email,
        Pool: userPool
      });

      cognitoUser.authenticateUser(authenticationDetails, {
        onSuccess: (session) => {
          cognitoUser.getUserAttributes((err, attributes) => {
            if (err) {
              reject(err);
              return;
            }

            const userAttributes = {};
            attributes.forEach(attr => {
              userAttributes[attr.getName()] = attr.getValue();
            });

            resolve({
              user: {
                username: cognitoUser.getUsername(),
                attributes: userAttributes
              },
              session
            });
          });
        },
        onFailure: (err) => {
          reject(err);
        },
        newPasswordRequired: (userAttributes, requiredAttributes) => {
          // Handle new password required scenario
          reject(new Error('New password required'));
        }
      });
    });
  }

  // Sign out user
  signOut() {
    return new Promise((resolve, reject) => {
      // Development mode bypass
      if (import.meta.env.VITE_AUTH_MODE === 'development') {
        resolve();
        return;
      }

      const cognitoUser = userPool.getCurrentUser();
      
      if (cognitoUser) {
        cognitoUser.signOut();
      }
      
      resolve();
    });
  }

  // Sign up new user
  signUp(email, password, attributes = {}) {
    return new Promise((resolve, reject) => {
      const attributeList = [];
      
      // Add email attribute
      attributeList.push(new CognitoUserAttribute({
        Name: 'email',
        Value: email
      }));

      // Add other attributes
      Object.keys(attributes).forEach(key => {
        attributeList.push(new CognitoUserAttribute({
          Name: key,
          Value: attributes[key]
        }));
      });

      userPool.signUp(email, password, attributeList, null, (err, result) => {
        if (err) {
          reject(err);
          return;
        }
        resolve(result);
      });
    });
  }

  // Confirm sign up with verification code
  confirmSignUp(email, code) {
    return new Promise((resolve, reject) => {
      const cognitoUser = new CognitoUser({
        Username: email,
        Pool: userPool
      });

      cognitoUser.confirmRegistration(code, true, (err, result) => {
        if (err) {
          reject(err);
          return;
        }
        resolve(result);
      });
    });
  }

  // Get JWT token for API calls
  getIdToken() {
    return new Promise((resolve, reject) => {
      // Development mode bypass
      if (import.meta.env.VITE_AUTH_MODE === 'development') {
        resolve('dev-token');
        return;
      }

      const cognitoUser = userPool.getCurrentUser();
      
      if (!cognitoUser) {
        reject(new Error('No current user'));
        return;
      }

      cognitoUser.getSession((err, session) => {
        if (err) {
          reject(err);
          return;
        }

        if (!session.isValid()) {
          reject(new Error('Session is not valid'));
          return;
        }

        resolve(session.getIdToken().getJwtToken());
      });
    });
  }
}

export const authService = new AuthService();