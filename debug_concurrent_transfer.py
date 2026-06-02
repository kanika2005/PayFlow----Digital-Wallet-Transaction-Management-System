import json
import urllib.error
import urllib.request
import hashlib
import hmac
import base64
import datetime

BASE = 'http://localhost:8080'
JWT_SECRET = 'change-this-secret-key-to-a-long-random-value-at-least-32-chars'


def http_request(method, path, body=None, token=None):
    url = BASE + path
    headers = {'Content-Type': 'application/json'}
    data = None
    if body is not None:
        data = json.dumps(body).encode('utf-8')
    if token:
        headers['Authorization'] = token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            txt = resp.read().decode('utf-8')
            try:
                return resp.status, json.loads(txt)
            except Exception:
                return resp.status, txt
    except urllib.error.HTTPError as e:
        txt = e.read().decode('utf-8')
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, txt
    except Exception as e:
        return 'ERROR', str(e)


if __name__ == '__main__':
    ts = datetime.datetime.utcnow().strftime('%Y%m%d%H%M%S')
    sender = f'conctest+{ts}@example.com'
    r1 = f'concrec+{ts}@example.com'
    r2 = f'concrec2+{ts}@example.com'
    print('register sender', http_request('POST', '/api/auth/register', {'fullName': 'Sender', 'email': sender, 'password': 'Password123!'}))
    print('register r1', http_request('POST', '/api/auth/register', {'fullName': 'Receiver1', 'email': r1, 'password': 'Password123!'}))
    print('register r2', http_request('POST', '/api/auth/register', {'fullName': 'Receiver2', 'email': r2, 'password': 'Password123!'}))
    status, body = http_request('POST', '/api/auth/login', {'email': sender, 'password': 'Password123!'})
    print('login sender', status, body)
    token = f"Bearer {body['token']}" if status == 200 and isinstance(body, dict) else None
    print('add money', http_request('POST', '/api/wallet/add-money', {'amount': 100.0}, token))
    print('balance', http_request('GET', '/api/wallet/balance', token=token))

    def transfer_to_receiver(email, amount, desc):
        return http_request('POST', '/api/wallet/transfer', {'receiverEmail': email, 'amount': amount, 'description': desc}, token)

    from concurrent.futures import ThreadPoolExecutor
    with ThreadPoolExecutor(max_workers=2) as executor:
        futures = [executor.submit(transfer_to_receiver, r1, 60.0, 't1'), executor.submit(transfer_to_receiver, r2, 60.0, 't2')]
        for f in futures:
            print('concurrent transfer result', f.result())
    print('final balance', http_request('GET', '/api/wallet/balance', token=token))
    print('final txs', http_request('GET', '/api/wallet/transactions', token=token))
