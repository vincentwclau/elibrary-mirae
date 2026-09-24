import { describe, expect, it, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../test/renderWithProviders';
import { BrowsePage } from './BrowsePage';
import { browseBooks } from '../api/books';
import type { Book, PageResponse } from '../types';

vi.mock('../api/books', () => ({
  browseBooks: vi.fn(),
}));

const mockBrowse = vi.mocked(browseBooks);

function page(content: Book[]): PageResponse<Book> {
  return { content, page: 0, size: 12, totalElements: content.length, totalPages: 1, last: true };
}

const book: Book = {
  id: 'book-1',
  title: 'Clean Code',
  author: 'Robert C. Martin',
  isbn: '9780132350884',
  description: 'A handbook.',
  category: 'Software',
  publishedYear: 2008,
  totalCopies: 3,
  availableCopies: 2,
  available: true,
};

describe('BrowsePage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders books returned by the API', async () => {
    mockBrowse.mockResolvedValue(page([book]));

    renderWithProviders(<BrowsePage />);

    expect(await screen.findByText('Clean Code')).toBeInTheDocument();
    expect(screen.getByText('Robert C. Martin')).toBeInTheDocument();
    expect(screen.getByText('2 available')).toBeInTheDocument();
  });

  it('shows an empty state when there are no results', async () => {
    mockBrowse.mockResolvedValue(page([]));

    renderWithProviders(<BrowsePage />);

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument();
  });
});
