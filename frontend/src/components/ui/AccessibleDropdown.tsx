import { useMemo, useRef, useState } from 'react';

interface Option {
  label: string;
  value: string;
}

interface AccessibleDropdownProps {
  label: string;
  value: string;
  options: Option[];
  onChange: (value: string) => void;
}

export function AccessibleDropdown({ label, value, options, onChange }: AccessibleDropdownProps) {
  const [open, setOpen] = useState(false);
  const listRef = useRef<HTMLUListElement | null>(null);

  const selectedLabel = useMemo(
    () => options.find((option) => option.value === value)?.label || label,
    [label, options, value],
  );

  function handleKeyDown(event: React.KeyboardEvent) {
    if (!open && (event.key === 'Enter' || event.key === 'ArrowDown')) {
      event.preventDefault();
      setOpen(true);
      return;
    }
    if (!open || !listRef.current) {
      return;
    }
    const items = Array.from(listRef.current.querySelectorAll<HTMLButtonElement>('button[data-option]'));
    const currentIndex = items.findIndex((button) => button === document.activeElement);
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      (items[currentIndex + 1] || items[0])?.focus();
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      (items[currentIndex - 1] || items[items.length - 1])?.focus();
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  }

  return (
    <div className="dropdown" onKeyDown={handleKeyDown}>
      <button className="btn btn-secondary dropdown-trigger" type="button" onClick={() => setOpen((prev) => !prev)}>
        {selectedLabel}
      </button>
      {open && (
        <ul className="dropdown-menu glass-panel" ref={listRef} role="listbox" aria-label={label}>
          {options.map((option) => (
            <li key={option.value}>
              <button
                type="button"
                data-option
                className="dropdown-item"
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
              >
                {option.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
