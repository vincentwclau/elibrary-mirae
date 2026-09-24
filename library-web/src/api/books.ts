import { api } from './client';
import type { Book, PageResponse } from '../types';

export interface BrowseParams {
  search?: string;
  category?: string;
  page?: number;
  size?: number;
}

export async function browseBooks(params: BrowseParams): Promise<PageResponse<Book>> {
  const { data } = await api.get<PageResponse<Book>>('/books', {
    params: {
      search: params.search || undefined,
      category: params.category || undefined,
      page: params.page ?? 0,
      size: params.size ?? 12,
    },
  });
  return data;
}

export async function getBook(id: string): Promise<Book> {
  const { data } = await api.get<Book>(`/books/${id}`);
  return data;
}
