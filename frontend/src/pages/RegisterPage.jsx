import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function RegisterPage() {
  const navigate = useNavigate();
  const { register, error, isLoading, setError } = useAuth();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [formError, setFormError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError('');
    setError(null);

    if (!fullName || !email || !password) {
      setFormError('All fields are required.');
      return;
    }

    try {
      await register({ fullName, email, password });
      navigate('/');
    } catch {
      setFormError('Unable to create account.');
    }
  };

  return (
    <section className="auth-page">
      <div className="panel">
        <h1>Create Account</h1>
        <p>Register to use the wallet system and manage your funds.</p>
        <form onSubmit={handleSubmit}>
          <label>
            Full Name
            <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
          </label>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
          </label>
          {(formError || error) && <p className="message error">{formError || error}</p>}
          <button type="submit" disabled={isLoading}>{isLoading ? 'Creating account...' : 'Register'}</button>
        </form>
        <p className="small-text">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </section>
  );
}

export default RegisterPage;
