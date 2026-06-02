import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function LoginPage() {
  const navigate = useNavigate();
  const { login, error, isLoading, setError } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [formError, setFormError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError('');
    setError(null);

    if (!email || !password) {
      setFormError('Please enter both email and password.');
      return;
    }

    try {
      await login({ email, password });
      navigate('/');
    } catch {
      setFormError('Unable to sign in. Check your credentials.');
    }
  };

  return (
    <section className="auth-page">
      <div className="panel">
        <h1>Sign In</h1>
        <p>Access your wallet and manage transfers.</p>
        <form onSubmit={handleSubmit}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {(formError || error) && <p className="message error">{formError || error}</p>}
          <button type="submit" disabled={isLoading}>{isLoading ? 'Signing in...' : 'Sign In'}</button>
        </form>
        <p className="small-text">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </section>
  );
}

export default LoginPage;
