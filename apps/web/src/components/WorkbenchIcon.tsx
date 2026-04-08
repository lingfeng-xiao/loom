import type { WorkbenchIconName } from '../frontendTypes'

interface WorkbenchIconProps {
  name: WorkbenchIconName
  size?: number
  className?: string
}

export function WorkbenchIcon({ name, size = 16, className }: WorkbenchIconProps) {
  const strokeProps = {
    fill: 'none',
    stroke: 'currentColor',
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    strokeWidth: 1.7,
  }

  return (
    <svg aria-hidden="true" className={className} height={size} viewBox="0 0 20 20" width={size}>
      {name === 'compose' ? (
        <>
          <path {...strokeProps} d="M4.5 14.5 13.75 5.25l1.5 1.5L6 16H4.5z" />
          <path {...strokeProps} d="m12.75 6.25 1.5-1.5a1.4 1.4 0 0 1 2 0l.5.5a1.4 1.4 0 0 1 0 2l-1.5 1.5" />
        </>
      ) : null}
      {name === 'spark' ? (
        <>
          <path {...strokeProps} d="M10 2.75v4.5" />
          <path {...strokeProps} d="M10 12.75v4.5" />
          <path {...strokeProps} d="M2.75 10h4.5" />
          <path {...strokeProps} d="M12.75 10h4.5" />
          <path {...strokeProps} d="m5.35 5.35 3.1 3.1" />
          <path {...strokeProps} d="m11.55 11.55 3.1 3.1" />
          <path {...strokeProps} d="m14.65 5.35-3.1 3.1" />
          <path {...strokeProps} d="m8.45 11.55-3.1 3.1" />
        </>
      ) : null}
      {name === 'automation' ? (
        <>
          <path {...strokeProps} d="M10 3.5 5 6.5v6L10 15.5l5-3v-6Z" />
          <path {...strokeProps} d="M10 3.5v12" />
          <path {...strokeProps} d="m5 6.5 5 3 5-3" />
        </>
      ) : null}
      {name === 'pulse' ? <path {...strokeProps} d="M3 10h3l1.5-3 2.5 6 1.75-4H17" /> : null}
      {name === 'settings' ? (
        <>
          <circle {...strokeProps} cx="10" cy="10" r="2.5" />
          <path {...strokeProps} d="M10 3.25v1.5" />
          <path {...strokeProps} d="M10 15.25v1.5" />
          <path {...strokeProps} d="m15.1 4.9-1.05 1.05" />
          <path {...strokeProps} d="m5.95 14.05-1.05 1.05" />
          <path {...strokeProps} d="M16.75 10h-1.5" />
          <path {...strokeProps} d="M4.75 10h-1.5" />
          <path {...strokeProps} d="m15.1 15.1-1.05-1.05" />
          <path {...strokeProps} d="M5.95 5.95 4.9 4.9" />
        </>
      ) : null}
      {name === 'search' ? (
        <>
          <circle {...strokeProps} cx="8.5" cy="8.5" r="4.25" />
          <path {...strokeProps} d="m11.7 11.7 4.05 4.05" />
        </>
      ) : null}
      {name === 'switch' ? (
        <>
          <path {...strokeProps} d="M4 6.25h10.5" />
          <path {...strokeProps} d="m11.75 3.5 2.75 2.75L11.75 9" />
          <path {...strokeProps} d="M16 13.75H5.5" />
          <path {...strokeProps} d="M8.25 11 5.5 13.75l2.75 2.75" />
        </>
      ) : null}
      {name === 'more' ? (
        <>
          <circle cx="5" cy="10" fill="currentColor" r="1.1" />
          <circle cx="10" cy="10" fill="currentColor" r="1.1" />
          <circle cx="15" cy="10" fill="currentColor" r="1.1" />
        </>
      ) : null}
      {name === 'submit' ? <path {...strokeProps} d="m4 10 4 4 8-8" /> : null}
      {name === 'thread' ? (
        <>
          <path {...strokeProps} d="M4.25 5.5h11.5" />
          <path {...strokeProps} d="M4.25 10h8.5" />
          <path {...strokeProps} d="M4.25 14.5h9.75" />
        </>
      ) : null}
      {name === 'status' ? (
        <>
          <circle {...strokeProps} cx="10" cy="10" r="5.5" />
          <path {...strokeProps} d="M10 6.5v4l2.25 1.25" />
        </>
      ) : null}
      {name === 'files' || name === 'folder' ? (
        <>
          <path {...strokeProps} d="M3.75 5.5h4l1.5 1.5h7v7.5H3.75z" />
          <path {...strokeProps} d="M3.75 7h12.5" />
        </>
      ) : null}
      {name === 'folderOpen' ? (
        <>
          <path {...strokeProps} d="M3.75 6h4l1.5 1.5h6.5l-1.25 6.5H5.25z" />
          <path {...strokeProps} d="M3.75 7.5h12" />
        </>
      ) : null}
      {name === 'memory' ? (
        <>
          <path {...strokeProps} d="M5 5.5A2.5 2.5 0 0 1 7.5 3h7.75v14H7.5A2.5 2.5 0 0 0 5 14.5Z" />
          <path {...strokeProps} d="M5 5.5v9" />
        </>
      ) : null}
      {name === 'chat' ? (
        <>
          <path {...strokeProps} d="M4 5.25h12v8.5H8.25L5 16v-2.25H4z" />
        </>
      ) : null}
      {name === 'chevronDown' ? <path {...strokeProps} d="m5.5 7.5 4.5 4.5 4.5-4.5" /> : null}
      {name === 'chevronRight' ? <path {...strokeProps} d="m7.5 5.5 4.5 4.5-4.5 4.5" /> : null}
      {name === 'plus' ? (
        <>
          <path {...strokeProps} d="M10 4v12" />
          <path {...strokeProps} d="M4 10h12" />
        </>
      ) : null}
      {name === 'sort' ? (
        <>
          <path {...strokeProps} d="M6 5.5h8" />
          <path {...strokeProps} d="M6 10h5.5" />
          <path {...strokeProps} d="M6 14.5h3" />
        </>
      ) : null}
      {name === 'paperclip' ? <path {...strokeProps} d="M7.5 10.5 12 6a2.5 2.5 0 1 1 3.5 3.5l-5.75 5.75a4 4 0 1 1-5.65-5.65L9.25 4.5" /> : null}
      {name === 'slash' ? <path {...strokeProps} d="M13.5 3.5 6.5 16.5" /> : null}
      {name === 'bolt' ? <path {...strokeProps} d="M10.75 3.5 5.5 10h3l-.25 6.5 5.25-6.5h-3z" /> : null}
      {name === 'send' ? <path {...strokeProps} d="m4 10 11-5-3 5 3 5Z" /> : null}
      {name === 'tasks' ? (
        <>
          <path {...strokeProps} d="M5 5.25h10" />
          <path {...strokeProps} d="M5 10h10" />
          <path {...strokeProps} d="M5 14.75h10" />
          <circle cx="3.25" cy="5.25" fill="currentColor" r="1" />
          <circle cx="3.25" cy="10" fill="currentColor" r="1" />
          <circle cx="3.25" cy="14.75" fill="currentColor" r="1" />
        </>
      ) : null}
      {name === 'terminal' ? (
        <>
          <rect {...strokeProps} height="11" rx="2" width="14" x="3" y="4.5" />
          <path {...strokeProps} d="m6.25 8 2 2-2 2" />
          <path {...strokeProps} d="M10.75 12H14" />
        </>
      ) : null}
      {name === 'panelCollapse' ? (
        <>
          <rect {...strokeProps} height="12" rx="2" width="14" x="3" y="4" />
          <path {...strokeProps} d="M8.5 6.5v7" />
          <path {...strokeProps} d="m12.5 10-2-2v4z" fill="currentColor" stroke="none" />
        </>
      ) : null}
      {name === 'panelExpand' ? (
        <>
          <rect {...strokeProps} height="12" rx="2" width="14" x="3" y="4" />
          <path {...strokeProps} d="M11.5 6.5v7" />
          <path {...strokeProps} d="m7.5 10 2-2v4z" fill="currentColor" stroke="none" />
        </>
      ) : null}
    </svg>
  )
}
