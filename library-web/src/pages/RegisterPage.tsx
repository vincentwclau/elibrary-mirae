import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { register } from '../api/auth';
import { toErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner } from '../components/StatusMessage';
import { AuthShell, Field } from './LoginPage';

export function RegisterPage() {
  const { onAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const mutation = useMutation({
    mutationFn: register,
    onSuccess: (auth) => {
      onAuthenticated(auth);
      navigate('/', { replace: true });
    },
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    mutation.mutate({ name, email, password });
  };

  return (
    <AuthShell title="Create your account">
      <form onSubmit={submit} className="space-y-4">
        {mutation.isError && (
          <ErrorBanner message={toErrorMessage(mutation.error, 'Registration failed')} />
        )}
        <Field label="Name" type="text" value={name} onChange={setName} autoComplete="name" />
        <Field label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
        <Field
          label="Password (min 8 characters)"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
        />
        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
        >
          {mutation.isPending ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-slate-900 underline">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
