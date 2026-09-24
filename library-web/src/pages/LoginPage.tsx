import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { login } from '../api/auth';
import { toErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner } from '../components/StatusMessage';

interface LocationState {
  from?: { pathname: string };
}

export function LoginPage() {
  const { onAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = (location.state as LocationState | null)?.from?.pathname ?? '/';

  const [email, setEmail] = useState('member@demo.io');
  const [password, setPassword] = useState('password123');

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (auth) => {
      onAuthenticated(auth);
      navigate(redirectTo, { replace: true });
    },
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    mutation.mutate({ email, password });
  };

  return (
    <AuthShell title="Sign in to your account">
      <form onSubmit={submit} className="space-y-4">
        {mutation.isError && <ErrorBanner message={toErrorMessage(mutation.error, 'Login failed')} />}
        <Field label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
        <Field
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
        />
        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
        >
          {mutation.isPending ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        No account?{' '}
        <Link to="/register" className="font-medium text-slate-900 underline">
          Create one
        </Link>
      </p>
    </AuthShell>
  );
}

export function AuthShell({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mx-auto mt-16 w-full max-w-sm px-4">
      <h1 className="mb-6 text-center text-2xl font-semibold text-slate-900">{title}</h1>
      <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">{children}</div>
    </div>
  );
}

interface FieldProps {
  label: string;
  type: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete?: string;
}

export function Field({ label, type, value, onChange, autoComplete }: FieldProps) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>
      <input
        type={type}
        value={value}
        autoComplete={autoComplete}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
      />
    </label>
  );
}
