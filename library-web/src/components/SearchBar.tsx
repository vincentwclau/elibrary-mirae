import { useState, type FormEvent } from 'react';

interface SearchBarProps {
  initialSearch?: string;
  initialCategory?: string;
  onSearch: (search: string, category: string) => void;
}

export function SearchBar({ initialSearch = '', initialCategory = '', onSearch }: SearchBarProps) {
  const [search, setSearch] = useState(initialSearch);
  const [category, setCategory] = useState(initialCategory);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSearch(search.trim(), category.trim());
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-3 sm:flex-row">
      <input
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Search by title or author"
        aria-label="Search by title or author"
        className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
      />
      <input
        type="text"
        value={category}
        onChange={(e) => setCategory(e.target.value)}
        placeholder="Category (e.g. Software)"
        aria-label="Filter by category"
        className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none sm:w-56"
      />
      <button
        type="submit"
        className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
      >
        Search
      </button>
    </form>
  );
}
