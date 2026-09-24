// Types mirroring the backend API DTOs (library-service).

export type Role = 'MEMBER' | 'ADMIN';
export type LoanStatus = 'ACTIVE' | 'RETURNED';

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  userId: string;
  email: string;
  name: string;
  role: Role;
}

export interface Book {
  id: string;
  title: string;
  author: string;
  isbn: string;
  description: string | null;
  category: string;
  publishedYear: number | null;
  totalCopies: number;
  availableCopies: number;
  available: boolean;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface Loan {
  id: string;
  status: LoanStatus;
  borrowedAt: string;
  dueAt: string;
  returnedAt: string | null;
  book: {
    id: string;
    title: string;
    author: string;
    isbn: string;
  };
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}
