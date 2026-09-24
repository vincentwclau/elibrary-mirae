import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
      isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
    }`;

  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <Link to="/" className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <span aria-hidden>📚</span> E-Library
        </Link>
        {isAuthenticated && (
          <div className="flex items-center gap-2">
            <NavLink to="/" end className={linkClass}>
              Browse
            </NavLink>
            <NavLink to="/my-loans" className={linkClass}>
              My Loans
            </NavLink>
            <span className="hidden sm:inline text-sm text-slate-500">{user?.name}</span>
            <button
              onClick={logout}
              className="ml-1 rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Sign out
            </button>
          </div>
        )}
      </nav>
    </header>
  );
}
