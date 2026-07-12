import { describe, it, expect } from 'vitest';

describe('Theme Bridge', () => {
  it('should have M3 system color tokens in :root', () => {
    const rootStyles = getComputedStyle(document.documentElement);
    expect(rootStyles.getPropertyValue('--sys-color-primary').trim()).toBe('#007fff');
  });

  it('should have legacy color tokens in :root', () => {
    const rootStyles = getComputedStyle(document.documentElement);
    expect(rootStyles.getPropertyValue('--color-primary').trim()).toBe('#007fff');
  });
});
