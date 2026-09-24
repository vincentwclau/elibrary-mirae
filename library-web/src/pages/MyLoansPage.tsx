import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getMyLoans, returnLoan } from '../api/loans';
import { toErrorMessage } from '../api/client';
import type { Loan, LoanStatus } from '../types';
import { EmptyState, ErrorBanner, Loading } from '../components/StatusMessage';

type Filter = 'ACTIVE' | 'ALL';

export function MyLoansPage() {
  const [filter, setFilter] = useState<Filter>('ACTIVE');
  const queryClient = useQueryClient();
  const statusParam: LoanStatus | undefined = filter === 'ACTIVE' ? 'ACTIVE' : undefined;

  const query = useQuery({
    queryKey: ['loans', filter],
    queryFn: () => getMyLoans(statusParam),
  });

  const returnMutation = useMutation({
    mutationFn: returnLoan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['books'] });
    },
  });

  return (
    <div className="mx-auto max-w-3xl space-y-6 px-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-900">My loans</h1>
        <div className="flex rounded-md border border-slate-300 p-0.5 text-sm">
          <TabButton active={filter === 'ACTIVE'} onClick={() => setFilter('ACTIVE')}>
            Currently borrowed
          </TabButton>
          <TabButton active={filter === 'ALL'} onClick={() => setFilter('ALL')}>
            All
          </TabButton>
        </div>
      </div>

      {returnMutation.isError && (
        <ErrorBanner message={toErrorMessage(returnMutation.error, 'Could not return the book')} />
      )}
      {query.isLoading && <Loading label="Loading loans…" />}
      {query.isError && <ErrorBanner message={toErrorMessage(query.error)} />}

      {query.data &&
        (query.data.length === 0 ? (
          <EmptyState
            message={filter === 'ACTIVE' ? 'You have no books borrowed.' : 'No loans yet.'}
          />
        ) : (
          <ul className="space-y-3">
            {query.data.map((loan) => (
              <LoanRow
                key={loan.id}
                loan={loan}
                onReturn={() => returnMutation.mutate(loan.id)}
                returning={returnMutation.isPending && returnMutation.variables === loan.id}
              />
            ))}
          </ul>
        ))}
    </div>
  );
}

function LoanRow({
  loan,
  onReturn,
  returning,
}: {
  loan: Loan;
  onReturn: () => void;
  returning: boolean;
}) {
  const active = loan.status === 'ACTIVE';
  return (
    <li className="flex items-center justify-between rounded-lg border border-slate-200 bg-white p-4">
      <div>
        <h3 className="font-semibold text-slate-900">{loan.book.title}</h3>
        <p className="text-sm text-slate-500">{loan.book.author}</p>
        <p className="mt-1 text-xs text-slate-400">
          {active
            ? `Due ${new Date(loan.dueAt).toLocaleDateString()}`
            : `Returned ${loan.returnedAt ? new Date(loan.returnedAt).toLocaleDateString() : ''}`}
        </p>
      </div>
      {active ? (
        <button
          onClick={onReturn}
          disabled={returning}
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
        >
          {returning ? 'Returning…' : 'Return'}
        </button>
      ) : (
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500">
          Returned
        </span>
      )}
    </li>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`rounded px-3 py-1.5 font-medium transition-colors ${
        active ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
      }`}
    >
      {children}
    </button>
  );
}
