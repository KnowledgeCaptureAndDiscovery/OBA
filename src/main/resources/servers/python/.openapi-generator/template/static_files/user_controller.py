import six
from jose import JWTError, jwt
import time
import json
import requests
from werkzeug.exceptions import Unauthorized

from openapi_server.settings import FIREBASE_KEY
JWT_ISSUER = 'com.zalando.connexion'
JWT_SECRET = 'change_this'
JWT_LIFETIME_SECONDS = 60000000
JWT_ALGORITHM = 'HS256'


def decode_token(token):
    try:
        return jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
    except JWTError as e:
        six.raise_from(Unauthorized, e)


def _current_timestamp() -> int:
    return int(time.time())

def auth_with_password(email, password):
    headers = {
        'Sec-Fetch-Mode': 'cors',
        'Content-Type': 'application/json',
    }

    params = (
        ('key', FIREBASE_KEY),
    )

    data = json.dumps({"email": email,"password": password,"returnSecureToken": True})

    response = requests.post('https://www.googleapis.com/identitytoolkit/v3/relyingparty/verifyPassword',
                             headers=headers, params=params, data=data)

    return response.ok

def user_login_get(username, password):  # noqa: E501
    """Logs user into the system

     # noqa: E501

    :param username: The user name for login
    :type username: str
    :param password: The password for login in clear text
    :type password: str

    :rtype: str
    """
    if not auth_with_password(username, password):
        return "Invalid User or Password", 401, {}


    timestamp = _current_timestamp()
    payload = {
        "iss": JWT_ISSUER,
        "iat": int(timestamp),
        "exp": int(timestamp + JWT_LIFETIME_SECONDS),
        "sub": str(username),
    }

    access_token = jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGORITHM)
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "expires_in": JWT_LIFETIME_SECONDS,
        "refresh_token": "IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk",
        "scope": "create"
    }, 200, {'X-Expires-After': JWT_LIFETIME_SECONDS, 'X-Rate-Limit': 1000}


