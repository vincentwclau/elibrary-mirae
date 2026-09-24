import { describe, expect, it, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { renderWithProviders } from '../test/renderWithProviders';
import { BookDetailPage } from './BookDetailPage';
import { getBook } from '../api/books';
import { borrowBook } from '../api/loans';
import type { Book, Loan } from '../types';

vi.mock('../api/books', () => ({ getBook: vi.fn() }));
vi.mock('../api/loans', () => ({ borrowBook: vi.fn() }));

const mockGetBook = vi.mocked(getBook);
const mockBorrow = vi.mocked(borrowBook);

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

function renderDetail() {
  return renderWithProviders(
    <Routes>
      <Route path="/books/:id" element={<BookDetailPage />} />
    </Routes>,
    { route: '/books/book-1' },
  );
}

describe('BookDetailPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('borrows the book and shows confirmation', async () => {
    mockGetBook.mockResolvedValue(book);
    const loan: Loan = {
      id: 'loan-1',
      status: 'ACTIVE',
      borrowedAt: '2026-01-01T10:00:00Z',
      dueAt: '2026-01-15T10:00:00Z',
      returnedAt: null,
      book: { id: book.id, title: book.title, author: book.author, isbn: book.isbn },
    };
    mockBorrow.mockResolvedValue(loan);

    renderDetail();

    const borrowButton = await screen.findByRole('button', { name: /borrow this book/i });
    await userEvent.click(borrowButton);

    expect(mockBorrow).toHaveBeenCalledWith('book-1');
    expect(await screen.findByText(/Borrowed!/i)).toBeInTheDocument();
  });

  it('disables borrowing when the book is unavailable', async () => {
    mockGetBook.mockResolvedValue({ ...book, available: false, availableCopies: 0 });

    renderDetail();

    const button = await screen.findByRole('button', { name: /unavailable/i });
    expect(button).toBeDisabled();
  });
});
