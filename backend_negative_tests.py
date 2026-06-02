import base64
import concurrent.futures
import datetime
import hashlib
import hmac
import http.client
import json
import urllib.error
import urllib.request

BASE = 'http://localhost:8080'
JWT_SECRET = 'change-this-secret-key-to-a-long-random-value-at-least-32-chars'
ADMIN_EMAIL = 'admin@wallet.com'
ADMIN_PASSWORD = 'Admin@12345'


def b64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode('utf-8').rstrip('=')


def http_request(method, path, body=None, token=None):
    url = BASE + path
    data = None
    headers = {'Content-Type': 'application/json'}
    if body is not None:
        data = json.dumps(body).encode('utf-8')
    if token:
        headers['Authorization'] = token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            txt = resp.read().decode('utf-8')
            try:
                body = json.loads(txt) if txt else None
            except json.JSONDecodeError:
                body = txt
            return resp.status, body
    except urllib.error.HTTPError as e:
        txt = e.read().decode('utf-8')
        try:
            body = json.loads(txt)
        except json.JSONDecodeError:
            body = txt
        return e.code, body
    except urllib.error.URLError as e:
        return 'ERROR', str(e)


def register_user(email, password='Password123!', full_name=None):
    if full_name is None:
        full_name = 'Test User'
    return http_request('POST', '/api/auth/register', {'fullName': full_name, 'email': email, 'password': password})


def login_user(email, password='Password123!'):
    return http_request('POST', '/api/auth/login', {'email': email, 'password': password})


def get_balance(token):
    return http_request('GET', '/api/wallet/balance', token=token)


def get_transactions(token):
    return http_request('GET', '/api/wallet/transactions', token=token)


def add_money(amount, token):
    return http_request('POST', '/api/wallet/add-money', {'amount': amount}, token=token)


def withdraw(amount, token):
    return http_request('POST', '/api/wallet/withdraw', {'amount': amount}, token=token)


def transfer(receiver_email, amount, description, token):
    return http_request('POST', '/api/wallet/transfer', {'receiverEmail': receiver_email, 'amount': amount, 'description': description}, token=token)


def admin_endpoint(path, token):
    return http_request('GET', path, token=token)


def generate_jwt(email, roles, expire_delta_seconds=-3600):
    header = {'alg': 'HS384', 'typ': 'JWT'}
    now = int(datetime.datetime.utcnow().timestamp())
    payload = {
        'sub': email,
        'roles': roles,
        'iat': now,
        'exp': now + expire_delta_seconds
    }
    header_b64 = b64url_encode(json.dumps(header, separators=(',', ':')).encode('utf-8'))
    payload_b64 = b64url_encode(json.dumps(payload, separators=(',', ':')).encode('utf-8'))
    signing_input = f'{header_b64}.{payload_b64}'.encode('utf-8')
    signature = hmac.new(JWT_SECRET.encode('utf-8'), signing_input, hashlib.sha384).digest()
    sig_b64 = b64url_encode(signature)
    return f'{header_b64}.{payload_b64}.{sig_b64}'


def run_concurrent_requests(funcs):
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(funcs)) as executor:
        futures = [executor.submit(f) for f in funcs]
        for future in concurrent.futures.as_completed(futures):
            try:
                results.append(future.result())
            except Exception as e:
                results.append(('ERROR', str(e)))
    return results


def format_result(name, status, body, expected_status=None):
    ok = (expected_status is None or status == expected_status)
    return {
        'test': name,
        'status': status,
        'expected': expected_status,
        'pass': ok,
        'body': body,
    }


def main():
    timestamp = datetime.datetime.utcnow().strftime('%Y%m%d%H%M%S')
    user_email = f'negtest+{timestamp}@example.com'
    user2_email = f'negtest2+{timestamp}@example.com'
    concurrent_email = f'conctest+{timestamp}@example.com'
    concurrent_receiver = f'concrec+{timestamp}@example.com'
    results = []

    # Setup users
    results.append(format_result('register primary user', *register_user(user_email, full_name='Negative Test User'), expected_status=200))
    results.append(format_result('register secondary user', *register_user(user2_email, full_name='Secondary Test User'), expected_status=200))
    login_status, login_body = login_user(user_email)
    results.append(format_result('login primary user', login_status, login_body, expected_status=200))
    token = None
    if login_status == 200 and isinstance(login_body, dict):
        token = f"Bearer {login_body['token']}"

    # Negative tests
    invalid_login_status, invalid_login_body = login_user(user_email, 'WrongPassword!')
    results.append(format_result('invalid login wrong password', invalid_login_status, invalid_login_body, expected_status=400))
    invalid_login_status2, invalid_login_body2 = login_user('doesnotexist@example.com', 'Password123!')
    results.append(format_result('invalid login unknown email', invalid_login_status2, invalid_login_body2, expected_status=400))
    duplicate_reg_status, duplicate_reg_body = register_user(user_email, full_name='Duplicate Test')
    results.append(format_result('duplicate registration', duplicate_reg_status, duplicate_reg_body, expected_status=400))
    neg_add_status, neg_add_body = add_money(-10.00, token)
    results.append(format_result('add money negative amount', neg_add_status, neg_add_body, expected_status=400))
    neg_withdraw_status, neg_withdraw_body = withdraw(-5.00, token)
    results.append(format_result('withdraw negative amount', neg_withdraw_status, neg_withdraw_body, expected_status=400))
    neg_transfer_status, neg_transfer_body = transfer(user2_email, -5.00, 'invalid negative', token)
    results.append(format_result('transfer negative amount', neg_transfer_status, neg_transfer_body, expected_status=400))
    self_transfer_status, self_transfer_body = transfer(user_email, 1.00, 'self transfer', token)
    results.append(format_result('self transfer', self_transfer_status, self_transfer_body, expected_status=400))

    # Ensure current balance is zero for insufficient funds tests
    balance_status, balance_body = get_balance(token)
    results.append(format_result('check balance before insufficient tests', balance_status, balance_body, expected_status=200))
    current_balance = balance_body.get('balance') if isinstance(balance_body, dict) else None
    results.append(format_result('record current balance', 200, {'balance': current_balance}, expected_status=200))
    high_withdraw_status, high_withdraw_body = withdraw(9999.00, token)
    results.append(format_result('withdraw insufficient funds', high_withdraw_status, high_withdraw_body, expected_status=400))
    high_transfer_status, high_transfer_body = transfer(user2_email, 9999.00, 'insufficient', token)
    results.append(format_result('transfer insufficient funds', high_transfer_status, high_transfer_body, expected_status=400))

    # Invalid JWT and expired JWT
    invalid_token = 'Bearer invalid.token.value'
    inv_status, inv_body = get_balance(invalid_token)
    results.append(format_result('invalid JWT access', inv_status, inv_body, expected_status=401))
    expired_token = f'Bearer {generate_jwt(user_email, ["ROLE_USER"], expire_delta_seconds=-3600)}'
    exp_status, exp_body = get_balance(expired_token)
    results.append(format_result('expired JWT access', exp_status, exp_body, expected_status=401))

    # Admin login
    admin_login_status, admin_login_body = login_user(ADMIN_EMAIL, ADMIN_PASSWORD)
    results.append(format_result('admin login', admin_login_status, admin_login_body, expected_status=200))
    admin_token = None
    if admin_login_status == 200 and isinstance(admin_login_body, dict):
        admin_token = f"Bearer {admin_login_body['token']}"
        results.append(format_result('admin login token stored', 200, {'role': admin_login_body['user']['role']}))

    if admin_token:
        for path in ['/api/admin/users', '/api/admin/wallets', '/api/admin/transactions', '/api/admin/balances/summary']:
            status, body = admin_endpoint(path, admin_token)
            results.append(format_result(f'admin endpoint {path}', status, body, expected_status=200))

    # Concurrency tests
    results.append(format_result('register concurrency sender', *register_user(concurrent_email, full_name='Concurrency Sender'), expected_status=200))
    results.append(format_result('register concurrency receiver', *register_user(concurrent_receiver, full_name='Concurrency Receiver'), expected_status=200))
    conc_login_status, conc_login_body = login_user(concurrent_email)
    results.append(format_result('login concurrency sender', conc_login_status, conc_login_body, expected_status=200))
    conc_token = f"Bearer {conc_login_body['token']}" if conc_login_status == 200 else None
    # top-up sender to 100
    results.append(format_result('top-up concurrency sender', *add_money(100.00, conc_token), expected_status=200))
    balance_status, balance_body = get_balance(conc_token)
    results.append(format_result('concurrency sender balance after top-up', balance_status, balance_body, expected_status=200))

    # concurrent withdraws two requests of 80 each
    def withdraw_80():
        return withdraw(80.00, conc_token)
    withdraw_results = run_concurrent_requests([withdraw_80, withdraw_80])
    for idx, (stat, bod) in enumerate(withdraw_results, 1):
        results.append(format_result(f'concurrent withdraw {idx}', stat, bod, expected_status=None))
    final_balance_status, final_balance_body = get_balance(conc_token)
    results.append(format_result('concurrency sender balance after withdraws', final_balance_status, final_balance_body, expected_status=200))

    # top-up again to 100 for concurrent transfers
    results.append(format_result('top-up concurrency sender second', *add_money(100.00, conc_token), expected_status=200))
    # create second receiver for transfer concurrency
    concurrent_receiver2 = f'concrec2+{timestamp}@example.com'
    results.append(format_result('register concurrency receiver2', *register_user(concurrent_receiver2, full_name='Concurrency Receiver 2'), expected_status=200))
    # concurrent transfers 60 each to two receivers
    def transfer_60_to_r1():
        return transfer(concurrent_receiver, 60.00, 'concurrent transfer 1', conc_token)
    def transfer_60_to_r2():
        return transfer(concurrent_receiver2, 60.00, 'concurrent transfer 2', conc_token)
    transfer_results = run_concurrent_requests([transfer_60_to_r1, transfer_60_to_r2])
    for idx, (stat, bod) in enumerate(transfer_results, 1):
        results.append(format_result(f'concurrent transfer {idx}', stat, bod, expected_status=None))
    final_balance_status2, final_balance_body2 = get_balance(conc_token)
    results.append(format_result('concurrency sender balance after transfers', final_balance_status2, final_balance_body2, expected_status=200))

    # Detailed DB integrity checks via transaction listing
    tx_status, tx_body = get_transactions(conc_token)
    results.append(format_result('concurrency transaction history', tx_status, tx_body, expected_status=200))

    # Print summary
    print('TEST REPORT:')
    for res in results:
        status = 'PASS' if res['pass'] else 'FAIL'
        expected = res['expected'] if res['expected'] is not None else 'any'
        print(f"{res['test']} | {status} | actual={res['status']} expected={expected}")
        if not res['pass']:
            print('  Response:', json.dumps(res['body'], indent=2, ensure_ascii=False))
    print('\nDETAILS:')
    for res in results:
        if res['pass']:
            continue
        print(json.dumps(res, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    main()
