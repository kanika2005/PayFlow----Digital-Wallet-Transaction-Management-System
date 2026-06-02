import json
import random
import string
import sys
from urllib import error, request
from urllib.request import Request

BASE_URL = 'http://127.0.0.1:8080'
ADMIN_EMAIL = 'admin@wallet.com'
ADMIN_PASSWORD = 'Admin@12345'

headers = {'Content-Type': 'application/json'}


def rand_email():
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=8))
    return f'testuser_{suffix}@wallet.com'


def request_json(method, path, body=None, token=None):
    url = BASE_URL + path
    hdrs = headers.copy()
    if token:
        hdrs['Authorization'] = f'Bearer {token}'
    data = None
    if body is not None:
        data = json.dumps(body).encode('utf-8')
    req = Request(url, data=data, headers=hdrs, method=method)
    try:
        with request.urlopen(req, timeout=20) as resp:
            text = resp.read().decode('utf-8', errors='replace')
            try:
                payload = json.loads(text)
            except Exception:
                payload = text
            return resp.status, payload
    except error.HTTPError as exc:
        body = exc.read().decode('utf-8', errors='replace')
        try:
            payload = json.loads(body)
        except Exception:
            payload = body
        return exc.code, payload
    except Exception as exc:
        return 'error', str(exc)


def print_result(name, result):
    status, payload = result
    print('---', name, '---')
    print(status)
    if isinstance(payload, (dict, list)):
        print(json.dumps(payload, indent=2))
    else:
        print(payload)
    print()


def check_endpoint(method, path, body=None, token=None, name=None):
    if name is None:
        name = f'{method} {path}'
    result = request_json(method, path, body=body, token=token)
    print_result(name, result)
    status = result[0]
    return isinstance(status, int) and 200 <= status < 300


def main():
    print('Verifying project endpoints at', BASE_URL)
    print()

    success = True

    # Root
    root_ok = check_endpoint('GET', '/', name='root GET /')
    if not root_ok:
        print('note: GET / is expected to return 403 when the app is secured.')
        print()

    # Register + login
    user_email = rand_email()
    user_password = 'Password1!'
    print('Using test account:', user_email)
    print()

    register_ok = check_endpoint('POST', '/api/auth/register', body={'fullName': 'Test User', 'email': user_email, 'password': user_password}, name='POST /api/auth/register')
    login_user_result = request_json('POST', '/api/auth/login', body={'email': user_email, 'password': user_password})
    print_result('POST /api/auth/login', login_user_result)
    user_token = None
    if isinstance(login_user_result[1], dict):
        user_token = login_user_result[1].get('token')
    login_user_ok = isinstance(login_user_result[0], int) and 200 <= login_user_result[0] < 300

    success &= register_ok and login_user_ok

    if user_token:
        success &= check_endpoint('GET', '/api/wallet/balance', token=user_token, name='GET /api/wallet/balance')
        success &= check_endpoint('POST', '/api/wallet/add-money', body={'amount': 100}, token=user_token, name='POST /api/wallet/add-money')
        success &= check_endpoint('POST', '/api/wallet/withdraw', body={'amount': 10}, token=user_token, name='POST /api/wallet/withdraw')
        success &= check_endpoint('POST', '/api/wallet/transfer', body={'receiverEmail': ADMIN_EMAIL, 'amount': 25, 'description': 'Automated transfer'}, token=user_token, name='POST /api/wallet/transfer')
        success &= check_endpoint('GET', '/api/wallet/transactions', token=user_token, name='GET /api/wallet/transactions')
    else:
        print('ERROR: user login failed; skipping wallet checks.')
        success = False

    admin_login_result = request_json('POST', '/api/auth/login', body={'email': ADMIN_EMAIL, 'password': ADMIN_PASSWORD})
    print_result('POST /api/auth/login (admin)', admin_login_result)
    admin_token = None
    if isinstance(admin_login_result[1], dict):
        admin_token = admin_login_result[1].get('token')
    admin_login_ok = isinstance(admin_login_result[0], int) and 200 <= admin_login_result[0] < 300
    success &= admin_login_ok

    if admin_token:
        success &= check_endpoint('GET', '/api/admin/users', token=admin_token, name='GET /api/admin/users')
        success &= check_endpoint('GET', '/api/admin/wallets', token=admin_token, name='GET /api/admin/wallets')
        success &= check_endpoint('GET', '/api/admin/transactions', token=admin_token, name='GET /api/admin/transactions')
        success &= check_endpoint('GET', '/api/admin/balances/summary', token=admin_token, name='GET /api/admin/balances/summary')
    else:
        print('ERROR: admin login failed; skipping admin checks.')
        success = False

    print('Verification complete.')
    if success:
        print('All checks passed.')
        return 0
    print('Some checks failed. Review the output above.')
    return 1


if __name__ == '__main__':
    sys.exit(main())
