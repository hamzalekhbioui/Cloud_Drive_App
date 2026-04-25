interface Props {
  checked: boolean
  onChange: (val: boolean) => void
  disabled?: boolean
  label?: string
  description?: string
  size?: 'sm' | 'md'
}

export default function ToggleSwitch({ checked, onChange, disabled, label, description, size = 'md' }: Props) {
  const small = size === 'sm'
  return (
    <label className={`sett-toggle-row${disabled ? ' disabled' : ''}`}>
      {(label || description) && (
        <div className="sett-toggle-text">
          {label      && <span className="sett-toggle-label">{label}</span>}
          {description && <span className="sett-toggle-desc">{description}</span>}
        </div>
      )}
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => !disabled && onChange(!checked)}
        className={`sett-toggle${checked ? ' on' : ''}${small ? ' sm' : ''}`}
      >
        <span className="sett-toggle-thumb" />
      </button>
    </label>
  )
}