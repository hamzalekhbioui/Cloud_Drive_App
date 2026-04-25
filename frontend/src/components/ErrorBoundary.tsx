import React from 'react'

type Props = {
  children: React.ReactNode
}

type State = {
  hasError: boolean
  message: string
}

export default class ErrorBoundary extends React.Component<Props, State> {
  state: State = {
    hasError: false,
    message: '',
  }

  static getDerivedStateFromError(error: Error): State {
    return {
      hasError: true,
      message: error.message || 'Unexpected render error',
    }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('UI render failure captured by ErrorBoundary', { error, info })
  }

  private reloadPage = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24, background: 'var(--surface)' }}>
          <div style={{ maxWidth: 560, width: '100%', border: '1px solid var(--line)', borderRadius: 14, padding: 20, background: 'var(--surface-2)' }}>
            <h1 style={{ margin: 0, marginBottom: 10, fontSize: 22 }}>Something went wrong</h1>
            <p style={{ margin: 0, color: 'var(--ink-3)', fontSize: 14, lineHeight: 1.5 }}>
              The app hit a rendering error. Try reloading this page. If this keeps happening, check the browser console for details.
            </p>
            {this.state.message && (
              <pre style={{ marginTop: 12, marginBottom: 0, whiteSpace: 'pre-wrap', fontSize: 12, color: 'var(--danger)' }}>
                {this.state.message}
              </pre>
            )}
            <button type="button" className="btn btn-accent" style={{ marginTop: 14 }} onClick={this.reloadPage}>
              Reload app
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}

