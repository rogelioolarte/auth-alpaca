import { HttpErrorResponse } from '@angular/common/http';
import { FormGroup } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

/**
 * Maps backend validation errors (HTTP 400) to FormGroup controls.
 * If an error key does not match any form control, it is shown as a generic toast.
 * 
 * @param error HTTP error response
 * @param form The reactive form where errors will be applied
 * @param toastService Notification service for unmapped errors
 */
export function handleBackendFormErrors(
  error: HttpErrorResponse, 
  form: FormGroup, 
  toastService: ToastrService
): void {
  // Only process if it's a 400 and the body is an object (error map)
  if (error.status !== 400 || !error.error || typeof error.error !== 'object' || 
    error.error?.['message']) {
    return;
  }

  const backendErrors = error.error;
  const unmappedErrors: string[] = [];

  Object.keys(backendErrors).forEach(key => {
    const control = form.get(key);
    if (control) {
      // Assign error to control using the 'serverError' key
      // This distinguishes server errors from local validation (required, email, etc.)
      control.setErrors({ ...control.errors, serverError: backendErrors[key] });
    } else {
      // Store errors that don't map to any control
      unmappedErrors.push(`${key}: ${backendErrors[key]}`);
    }
  });

  // Show unmapped errors globally to avoid silencing backend errors
  if (unmappedErrors.length > 0) {
    toastService.error(
      `Validation errors: ${unmappedErrors.join(', ')}`, 
      'Form Error'
    );
  }
}

/**
 * Sets up listeners to clear 'serverError' when the user modifies the input.
 * Uses takeUntilDestroyed to prevent memory leaks.
 * 
 * @param form The reactive form to monitor
 * @param destroyRef The DestroyRef of the component for subscription cleanup
 */
export function setupServerErrorClearing(form: FormGroup, destroyRef: DestroyRef): void {
  Object.keys(form.controls).forEach(key => {
    const control = form.get(key);
    control?.valueChanges
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe(() => {
        if (control?.hasError('serverError')) {
          const currentErrors = { ...control.errors };
          delete currentErrors['serverError'];
          
          // Reset errors to remaining validators or null if none left
          control.setErrors(Object.keys(currentErrors).length > 0 ? currentErrors : null);
        }
      });
  });
}
