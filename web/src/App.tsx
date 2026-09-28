import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { supportedLanguages } from './i18n'

type Status = 'checking' | 'up' | 'down'
type Me = { username: string; name: string | null; roles: string[] }

// Spring sets this cookie. It is readable on purpose: it proves a request came from our own page.
function csrfToken(): string {
  return document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)?.[1] ?? ''
}

function App() {
  const { t, i18n } = useTranslation()
  const [status, setStatus] = useState<Status>('checking')
  const [me, setMe] = useState<Me | null | undefined>(undefined)

  useEffect(() => {
    // In development Vite forwards this to Spring Boot. In production Spring Boot serves both.
    fetch('/actuator/health')
      .then((response) => response.json())
      .then((body: { status: string }) => setStatus(body.status === 'UP' ? 'up' : 'down'))
      .catch(() => setStatus('down'))

    // The session cookie goes along automatically. The browser never sees a token.
    fetch('/api/me')
      .then((response) => (response.ok ? response.json() : null))
      .then((body: Me | null) => setMe(body))
      .catch(() => setMe(null))
  }, [])

  return (
    <main style={{ padding: '2rem', fontFamily: 'system-ui' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
        <h1>{t('app.name')}</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <label>
            {t('app.language')}{' '}
            <select value={i18n.resolvedLanguage} onChange={(event) => i18n.changeLanguage(event.target.value)}>
              {supportedLanguages.map((language) => (
                <option key={language} value={language}>
                  {language.toUpperCase()}
                </option>
              ))}
            </select>
          </label>
          {me === null && <a href="/oauth2/authorization/oidc">{t('auth.signIn')}</a>}
          {me && (
            // A plain form post, so the browser can follow the redirect to Keycloak's sign-out page.
            <form method="post" action="/logout">
              <input type="hidden" name="_csrf" value={csrfToken()} />
              <button type="submit">{t('auth.signOut')}</button>
            </form>
          )}
        </div>
      </header>
      <p>
        {t('status.title')}: {t(`status.${status}`)}
      </p>
      {me && (
        <p>
          {t('auth.signedInAs', { name: me.name ?? me.username })} · {t('auth.roles')}: {me.roles.join(', ') || '-'}
        </p>
      )}
    </main>
  )
}

export default App
