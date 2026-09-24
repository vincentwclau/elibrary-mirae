import { Link } from 'react-router-dom';
import type { Book } from '../types';

interface BookCardProps {
  book: Book;
}

export function BookCard({ book }: BookCardProps) {
  return (
    <Link
      to={`/books/${book.id}`}
      className="flex flex-col rounded-lg border border-slate-200 bg-white p-4 transition-shadow hover:shadow-md"
    >
      <div className="mb-2 flex items-start justify-between gap-2">
        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
          {book.category}
        </span>
        <AvailabilityBadge available={book.available} count={book.availableCopies} />
      </div>
      <h3 className="text-base font-semibold text-slate-900">{book.title}</h3>
      <p className="mt-0.5 text-sm text-slate-500">{book.author}</p>
      {book.publishedYear && <p className="mt-auto pt-3 text-xs text-slate-400">{book.publishedYear}</p>}
    </Link>
  );
}

export function AvailabilityBadge({ available, count }: { available: boolean; count: number }) {
  return available ? (
    <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700">
      {count} available
    </span>
  ) : (
    <span className="rounded-full bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-700">
      All borrowed
    </span>
  );
}
