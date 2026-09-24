import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { browseBooks } from '../api/books';
import { toErrorMessage } from '../api/client';
import { BookCard } from '../components/BookCard';
import { SearchBar } from '../components/SearchBar';
import { Pagination } from '../components/Pagination';
import { EmptyState, ErrorBanner, Loading } from '../components/StatusMessage';

interface Filters {
  search: string;
  category: string;
  page: number;
}

export function BrowsePage() {
  const [filters, setFilters] = useState<Filters>({ search: '', category: '', page: 0 });

  const query = useQuery({
    queryKey: ['books', filters],
    queryFn: () => browseBooks(filters),
    placeholderData: keepPreviousData,
  });

  const onSearch = (search: string, category: string) =>
    setFilters({ search, category, page: 0 });

  const onPageChange = (page: number) => setFilters((prev) => ({ ...prev, page }));

  return (
    <div className="mx-auto max-w-5xl space-y-6 px-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Browse the catalogue</h1>
        <p className="mt-1 text-sm text-slate-500">Find a book or journal and borrow it.</p>
      </div>

      <SearchBar
        initialSearch={filters.search}
        initialCategory={filters.category}
        onSearch={onSearch}
      />

      {query.isLoading && <Loading label="Loading books…" />}
      {query.isError && <ErrorBanner message={toErrorMessage(query.error)} />}

      {query.data && (
        <>
          {query.data.content.length === 0 ? (
            <EmptyState message="No books match your search." />
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {query.data.content.map((book) => (
                <BookCard key={book.id} book={book} />
              ))}
            </div>
          )}
          <Pagination
            page={query.data.page}
            totalPages={query.data.totalPages}
            onPageChange={onPageChange}
          />
        </>
      )}
    </div>
  );
}
