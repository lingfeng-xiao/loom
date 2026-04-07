import { AlertTriangle, Loader2, type LucideIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'

export interface StatusCardProps {
  icon?: LucideIcon
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
  loading?: boolean
}

export function StatusCard({
  icon: Icon = AlertTriangle,
  title,
  description,
  actionLabel,
  onAction,
  loading = false,
}: StatusCardProps) {
  return (
    <div className="rounded-2xl border bg-background/70 p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
          {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Icon className="h-4 w-4" />}
        </div>
        <div className="min-w-0 flex-1 space-y-2">
          <div className="text-sm font-medium leading-6">{title}</div>
          <div className="text-sm leading-6 text-muted-foreground">{description}</div>
          {actionLabel && onAction && (
            <Button variant="outline" size="sm" onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
