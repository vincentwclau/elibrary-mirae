import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getBook } from '../api/books';
import { borrowBook } from '../api/loans';
import { toErrorMessage } from '../api/client';
import { AvailabilityBadge } from '../components/BookCard';
import { ErrorBanner, Loading } from '../components/StatusMessage';

export function BookDetailPage() {
  const { id = '' } = useParams();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['book', id],
    queryFn: () => getBook(id),
  });

  const borrow = useMutation({
    mutationFn: () => borrowBook(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['book', id] });
      queryClient.invalidateQueries({ queryKey: ['books'] });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
    },
  });

  if (query.isLoading) {
    return <Loading label="Loading book…" />;
  }
  if (query.isError || !query.data) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <ErrorBanner message={toErrorMessage(query.error, 'Book not found')} />
        <BackLink />
      </div>
    );
  }

  const book = query.data;

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <BackLink />
      <div className="mt-4 rounded-xl border border-slate-200 bg-white p-6">
        <div className="mb-3 flex items-center gap-2">
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
            {book.category}
          </span>
          <AvailabilityBadge available={book.available} count={book.availableCopies} />
        </div>
        <h1 className="text-2xl font-semibold text-slate-900">{book.title}</h1>
        <p className="mt-1 text-slate-500">
          by {book.author}
          {book.publishedYear ? ` · ${book.publishedYear}` : ''}
        </p>
        <dl className="mt-4 text-sm text-slate-500">
          <dt className="inline font-medium text-slate-700">ISBN: </dt>
          <dd className="inline">{book.isbn}</dd>
        </dl>
        {book.description && <p className="mt-4 text-slate-700">{book.description}</p>}

        <div className="mt-6 space-y-3">
          {borrow.isError && <ErrorBanner message={toErrorMessage(borrow.error, 'Could not borrow')} />}
          {borrow.isSuccess && (
            <p className="text-sm font-medium text-emerald-700">
              Borrowed! Due {new Date(borrow.data.dueAt).toLocaleDateString()}.
            </p>
          )}
          <button
            onClick={() => borrow.mutate()}
            disabled={!book.available || borrow.isPending || borrow.isSuccess}
            className="rounded-md bg-slate-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {book.available ? (borrow.isPending ? 'Borrowing…' : 'Borrow this book') : 'Unavailable'}
          </button>
        </div>
      </div>
    </div>
  );
}

function BackLink() {
  return (
    <Link to="/" className="text-sm font-medium text-slate-600 hover:text-slate-900">
      ← Back to catalogue
    </Link>
  );
}
