import { useState } from 'react'

import type { Me } from '../api/meApi'

export function AccountPage({ me, onLogout, onDisconnect }: { me: Me; onLogout: () => void; onDisconnect: () => void }) {
  const [confirming, setConfirming] = useState(false)

  return (
    <section className="page">
      <h1>Conta</h1>
      <div className="profile">
        {me.pictureUrl ? <img src={me.pictureUrl} alt="" className="avatar large" referrerPolicy="no-referrer" /> : <span className="avatar large">{initial(me)}</span>}
        <div>
          <strong>{me.displayName || me.email}</strong>
          <div className="muted">{me.email}</div>
        </div>
      </div>
      <div className="account-actions">
        <div>
          <h2>Sair</h2>
          <p className="muted">Encerra a sessão neste navegador. Suas permissões no Google continuam valendo.</p>
          <button type="button" className="button" onClick={onLogout}>
            Sair
          </button>
        </div>
        <div>
          <h2>Desconectar conta Google</h2>
          <p className="muted">Revoga o acesso do Nexo no Google, apaga o token guardado e desliga todos os contextos.</p>
          {confirming ? (
            <div className="row">
              <button type="button" className="button button-danger" onClick={onDisconnect}>
                Confirmar desconexão
              </button>
              <button type="button" className="button" onClick={() => setConfirming(false)}>
                Cancelar
              </button>
            </div>
          ) : (
            <button type="button" className="button button-danger-outline" onClick={() => setConfirming(true)}>
              Desconectar
            </button>
          )}
        </div>
      </div>
    </section>
  )
}

export function initial(me: Me): string {
  return (me.displayName || me.email).charAt(0).toUpperCase()
}
