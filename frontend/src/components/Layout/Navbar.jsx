import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

function Navbar() {
  const { authUser, logout } = useAuth();

  return (
    <header className="topbar">
      <div className="brand">
        <Link to="/">Digital Wallet</Link>
      </div>
      <nav>
        {authUser ? (
          <>
            <span className="user-label">{authUser.fullName}</span>
            <button type="button" className="link-button" onClick={logout}>
              Sign out
            </button>
          </>
        ) : (
          <div className="nav-links">
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </div>
        )}
      </nav>
    </header>
  );
}

export default Navbar;
