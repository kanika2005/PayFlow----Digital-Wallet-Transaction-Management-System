import { Link } from 'react-router-dom';

function NotFoundPage() {
  return (
    <section className="notfound-page">
      <div className="panel">
        <h1>Page not found</h1>
        <p>The page you are looking for does not exist.</p>
        <Link to="/">Return to dashboard</Link>
      </div>
    </section>
  );
}

export default NotFoundPage;
