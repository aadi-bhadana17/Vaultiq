interface ToastProps {
  message: string;
  tone?: 'success' | 'error' | 'info';
  onClose: () => void;
}

export function Toast({ message, tone = 'info', onClose }: ToastProps) {
  return (
    <div className={`toast toast-${tone} slide-up`} role="status" aria-live="polite">
      <span>{message}</span>
      <button type="button" onClick={onClose} aria-label="Close notification">x</button>
    </div>
  );
}
