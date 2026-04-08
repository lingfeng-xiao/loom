interface WelcomePageProps {
  onEnter: () => void
}

export function WelcomePage({ onEnter }: WelcomePageProps) {
  return (
    <main className="welcomeShell">
      <section className="loadingState welcomePage">
        <p className="eyebrow">loom / 欢迎</p>
        <h1>以线程为中心的 AI 工作台</h1>
        <p>统一管理会话、执行轨迹、上下文和工具域，让工作区持续可演进。</p>
        <button className="primaryButton" onClick={onEnter} type="button">
          进入工作台
        </button>
      </section>
    </main>
  )
}
