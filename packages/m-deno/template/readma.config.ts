const name = '{{project-name}}'
const author = '{{readma-author-name}}'
const urls = {
  doc: `https://${name}.vercel.app/`,
}
const config = {
  language: 'ts',
  title: '{{readma-title}}',
  author,
  githubUsername: author,
  repoName: name,
  xHandle: author,
  domain: '{{readma-author-email-domain}}',
  domainExt: '{{readma-author-email-ext}}',
  email: author,
  urls,
  repobeats: '{{readma-repobeats}}',
  images: { screenshot: 'images/screenshot.gif', logo: 'images/logo.png' },
  sections: {
    features: '',
    projectDescription: '{{description}}',
    about: '',
    installation: '```deno install @{{scope}}/{{project-name}}```',
    acknowledgments: '',
    gettingStarted: 'See screenshot',
    usage: `🚧 In construction, refer to the [docs](${urls.doc})`,
    roadmap: '',
    '⛑️ Support':
      'Software is still **very early** expect **unexpected breaking changes**',
  },
  template: {
    bugReport: 'bug-report.yml',
    featRequest: 'feature-request.yml',
  },
  backToTop: false,
} as const
export default config
