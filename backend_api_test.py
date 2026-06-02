import json
import urllib.request
import urllib.error
import datetime

base = 'http://localhost:8080'
now = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
user1 = {'fullName': 'API Test User', 'email': f'apitest+{now}@example.com', 'password': 'Password123!'}
user2 = {'fullName': 'Recipient User', 'email': f'recipient+{now}@example.com', 'password': 'Password123!'}


def request(method, uri, body=None, token=None):
    url = base + uri
    data = None
    headers = {'Content-Type': 'application/json'}
    if body is not None:
        data = json.dumps(body).encode('utf-8')
    if token:
        headers['Authorization'] = token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode('utf-8')
            try:
                body = json.loads(raw) if raw else None
            except json.JSONDecodeError:
                body = raw
            return resp.status, body
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8')
        try:
            body = json.loads(raw)
        except Exception:
            body = raw
        return e.code, body
    except Exception as e:
        return 'ERROR', str(e)


def print_result(label, status, body):
    print(f'=== {label} ===')
    print('Status:', status)
    print('Body:')
    print(json.dumps(body, indent=2, ensure_ascii=False))
    print()


if __name__ == '__main__':
    status, body = request('POST', '/api/auth/register', user1)
    print_result('REGISTER user1', status, body)

    status, body = request('POST', '/api/auth/register', user2)
    print_result('REGISTER user2', status, body)

    status, body = request('POST', '/api/auth/login', {'email': user1['email'], 'password': user1['password']})
    print_result('LOGIN user1', status, body)
    token = None
    if status == 200 and isinstance(body, dict):
        token = f"{body.get('tokenType')} {body.get('token')}"
        print('Captured token:', token)
        print()

    status, body = request('GET', '/api/wallet/balance', token=token)
    print_result('BALANCE', status, body)

    status, body = request('GET', '/api/wallet/transactions', token=token)
    print_result('TRANSACTIONS initial', status, body)

    status, body = request('POST', '/api/wallet/add-money', {'amount': 100.00}, token)
    print_result('ADD MONEY', status, body)

    status, body = request('POST', '/api/wallet/withdraw', {'amount': 10.00}, token)
    print_result('WITHDRAW', status, body)

    status, body = request('POST', '/api/wallet/transfer', {'receiverEmail': user2['email'], 'amount': 5.00, 'description': 'API transfer test'}, token)
    print_result('TRANSFER', status, body)

    status, body = request('GET', '/api/wallet/transactions', token=token)
    print_result('TRANSACTIONS after ops', status, body)

    status, body = request('GET', '/api/admin/users', token=token)
    print_result('ADMIN users', status, body)

    status, body = request('GET', '/api/admin/wallets', token=token)
    print_result('ADMIN wallets', status, body)

    status, body = request('GET', '/api/admin/transactions', token=token)
    print_result('ADMIN transactions', status, body)

    status, body = request('GET', '/api/admin/balances/summary', token=token)
    print_result('ADMIN summary', status, body)
