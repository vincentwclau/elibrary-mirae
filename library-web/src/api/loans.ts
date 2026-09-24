import { api } from './client';
import type { Loan, LoanStatus } from '../types';

export async function getMyLoans(status?: LoanStatus): Promise<Loan[]> {
  const { data } = await api.get<Loan[]>('/loans/me', {
    params: { status: status || undefined },
  });
  return data;
}

export async function borrowBook(bookId: string): Promise<Loan> {
  const { data } = await api.post<Loan>('/loans', { bookId });
  return data;
}

export async function returnLoan(loanId: string): Promise<Loan> {
  const { data } = await api.post<Loan>(`/loans/${loanId}/return`);
  return data;
}
