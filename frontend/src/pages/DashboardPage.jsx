import { useEffect, useMemo, useState } from 'react';
import apiClient from '../api/api';
import { useAuth } from '../context/AuthContext';

function DashboardPage() {
  const { authUser } = useAuth();
  const [balance, setBalance] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [amount, setAmount] = useState('');
  const [withdraw, setWithdraw] = useState('');
  const [receiverEmail, setReceiverEmail] = useState('');
  const [transferAmount, setTransferAmount] = useState('');
  const [description, setDescription] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [balanceResponse, txResponse] = await Promise.all([
        apiClient.get('/wallet/balance'),
        apiClient.get('/wallet/transactions'),
      ]);
      setBalance(balanceResponse.data.balance);
      setTransactions(txResponse.data || []);
    } catch (e) {
      setError('Unable to load wallet data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleAddMoney = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');

    const value = parseFloat(amount);
    if (!value || value <= 0) {
      setError('Please enter a valid amount to add.');
      return;
    }

    try {
      await apiClient.post('/wallet/add-money', { amount: value });
      setMessage('Money added successfully.');
      setAmount('');
      fetchData();
    } catch {
      setError('Unable to add money.');
    }
  };

  const handleWithdraw = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');

    const value = parseFloat(withdraw);
    if (!value || value <= 0) {
      setError('Please enter a valid withdrawal amount.');
      return;
    }

    try {
      await apiClient.post('/wallet/withdraw', { amount: value });
      setMessage('Withdrawal completed successfully.');
      setWithdraw('');
      fetchData();
    } catch {
      setError('Unable to withdraw funds.');
    }
  };

  const handleTransfer = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');

    const value = parseFloat(transferAmount);
    if (!receiverEmail) {
      setError('Please enter the receiver email.');
      return;
    }
    if (!value || value <= 0) {
      setError('Please enter a valid transfer amount.');
      return;
    }

    try {
      await apiClient.post('/wallet/transfer', {
        receiverEmail,
        amount: value,
        description,
      });
      setMessage('Transfer completed successfully.');
      setReceiverEmail('');
      setTransferAmount('');
      setDescription('');
      fetchData();
    } catch {
      setError('Unable to complete transfer.');
    }
  };

  const sortedTransactions = useMemo(
    () => [...transactions].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)),
    [transactions]
  );

  return (
    <section className="dashboard-page">
      <header className="hero-panel">
        <div>
          <p className="eyebrow">Welcome back</p>
          <h1>{authUser?.fullName || 'Wallet User'}</h1>
          <p className="subtitle">Manage balance, transfers and transaction history from one place.</p>
        </div>
        <div className="balance-card">
          <span>Current balance</span>
          <strong>{balance !== null ? `$${Number(balance).toFixed(2)}` : 'Loading...'}</strong>
        </div>
      </header>

      <div className="grid-two-columns">
        <div className="panel form-panel">
          <h2>Quick wallet actions</h2>
          <form onSubmit={handleAddMoney}>
            <label>
              Add money
              <input type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />
            </label>
            <button type="submit">Add money</button>
          </form>

          <form onSubmit={handleWithdraw}>
            <label>
              Withdraw
              <input type="number" min="0.01" step="0.01" value={withdraw} onChange={(e) => setWithdraw(e.target.value)} />
            </label>
            <button type="submit">Withdraw</button>
          </form>

          <form onSubmit={handleTransfer}>
            <label>
              Destination email
              <input type="email" value={receiverEmail} onChange={(e) => setReceiverEmail(e.target.value)} />
            </label>
            <label>
              Amount
              <input type="number" min="0.01" step="0.01" value={transferAmount} onChange={(e) => setTransferAmount(e.target.value)} />
            </label>
            <label>
              Description
              <input type="text" value={description} onChange={(e) => setDescription(e.target.value)} />
            </label>
            <button type="submit">Transfer</button>
          </form>
        </div>

        <div className="panel transaction-panel">
          <h2>Recent transactions</h2>
          {loading && <p>Loading transactions...</p>}
          {error && <p className="message error">{error}</p>}
          {message && <p className="message success">{message}</p>}

          {sortedTransactions.length === 0 ? (
            <p>No transactions yet. Use the forms to add money or transfer funds.</p>
          ) : (
            <div className="transactions-list">
              {sortedTransactions.slice(0, 8).map((tx) => (
                <article key={tx.transactionId} className="transaction-card">
                  <div>
                    <p className="transaction-type">{tx.type}</p>
                    <p>{tx.description || 'No description'}</p>
                  </div>
                  <div className="transaction-meta">
                    <span>{tx.status}</span>
                    <small>{new Date(tx.createdAt).toLocaleString()}</small>
                    <strong>{tx.type === 'TRANSFER' ? `$${Number(tx.amount).toFixed(2)}` : `$${Number(tx.amount).toFixed(2)}`}</strong>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

export default DashboardPage;
