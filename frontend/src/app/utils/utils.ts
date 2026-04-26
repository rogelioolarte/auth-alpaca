import { HttpErrorResponse } from '@angular/common/http';

const defaultError = 'Request failed. Please try again.';

const httpErrors: Record<string, string> = {
  '0': 'The action could not be performed. Please check your connection.',
  '400': 'Request failed. Please try again.',
  '401': 'Incorrect credentials.',
  '403': 'Access not permitted.',
  '404': 'No data found.',
  '409': 'A conflict occurred. Please try again.',
  '429': 'Service overloaded. Please contact the administrator.',
  '444': 'The service did not respond.',
  '500': 'Server error. Please report to the administrator.',
  '503': 'Service unavailable.',
};

interface ToastError {
  error: HttpErrorResponse;
  defaultMessage?: string;
  statusMessages?: Record<string, string>;
  logger?: (error: HttpErrorResponse) => void;
}

export default function manageError({ error, defaultMessage, statusMessages }: ToastError): string {
  const statusKey = String(typeof error?.status === 'number' ? error.status : 0);
  // Use message when it's 400 error, because API Sunat return spanish message
  const message =
    (statusKey === '400' ? String(error.error.message || '') : '') ||
    statusMessages?.[statusKey] ||
    defaultMessage ||
    httpErrors[statusKey] ||
    defaultError;

  return message;
}
