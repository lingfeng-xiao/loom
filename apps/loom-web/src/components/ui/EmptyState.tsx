import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Inbox, type LucideIcon } from 'lucide-react'

interface EmptyStateProps {
  icon?: LucideIcon
  iconClassName?: string
  title: string
  description?: string
  action?: { label: string; onClick: () => void }
  size?: 'sm' | 'default' | 'lg'
  className?: string
}

const sizeIconMap = {
  sm: 'h-8 w-8',
  default: 'h-12 w-12',
  lg: 'h-16 w-16',
}

const sizePyMap = {
  sm: 'py-8',
  default: 'py-12',
  lg: 'py-16',
}

export function EmptyState({
  icon: Icon = Inbox,
  iconClassName,
  title,
  description,
  action,
  size = 'default',
  className,
}: EmptyStateProps) {
  return (
    <div className={cn(
      'flex flex-col items-center justify-center text-center px-4',
      sizePyMap[size],
      className
    )}>
      <Icon className={cn(
        'text-muted-foreground',
        sizeIconMap[size],
        iconClassName
      )} />
      <h3 className="mt-4 text-lg font-semibold">{title}</h3>
      {description && (
        <p className="mt-1 text-sm text-muted-foreground max-w-sm">
          {description}
        </p>
      )}
      {action && (
        <Button
          onClick={action.onClick}
          variant="outline"
          className="mt-4"
        >
          {action.label}
        </Button>
      )}
    </div>
  )
}
