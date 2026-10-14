import { fireEvent, render, screen } from '@testing-library/react'

import type { TimelineItem } from '../domain/conversationReducer'
import { ApprovalCard } from './ApprovalCard'

type Approval = Extract<TimelineItem, { kind: 'approval' }>

const base: Approval = {
  kind: 'approval',
  seq: 6,
  approvalId: 'a-1',
  tool: 'google_drive_delete_file',
  status: 'pending',
  severity: 'danger',
  description: 'Apagar para sempre “orçamento.xlsx” (não dá para desfazer)',
  args: { fileId: 'f1' },
  expiresAt: '2026-10-13T14:18:12Z',
}

describe('ApprovalCard', () => {
  it('em danger, aprovar pede um segundo clique', () => {
    const decide = vi.fn()

    render(<ApprovalCard item={base} onDecide={decide} />)
    fireEvent.click(screen.getByRole('button', { name: 'Aprovar exclusão' }))

    expect(decide).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Clique de novo para confirmar' }))

    expect(decide).toHaveBeenCalledWith('a-1', 'approve')
  })

  it('em warning, um clique basta; recusar também', () => {
    const decide = vi.fn()

    render(<ApprovalCard item={{ ...base, severity: 'warning' }} onDecide={decide} />)
    fireEvent.click(screen.getByRole('button', { name: 'Recusar' }))
    fireEvent.click(screen.getByRole('button', { name: 'Aprovar' }))

    expect(decide.mock.calls).toEqual([
      ['a-1', 'reject'],
      ['a-1', 'approve'],
    ])
  })

  it('depois de resolvida, os botões somem e fica o resultado', () => {
    render(<ApprovalCard item={{ ...base, status: 'expired' }} onDecide={vi.fn()} />)

    expect(screen.queryByRole('button')).toBeNull()
    expect(screen.getByText('Venceu sem resposta')).toBeInTheDocument()
    expect(screen.getByText(/orçamento\.xlsx/)).toBeInTheDocument()
  })
})
