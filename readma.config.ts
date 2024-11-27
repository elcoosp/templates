const name = 'templates';
const author = 'elcoosp';
const config = {
  language: 'ts',
  title: 'Templates',
  author,
  githubUsername: author,
  repoName: name,
  xHandle: author,
  domain: 'gmail',
  email: author,
  urls: {
    doc: 'https://github.com/elcoosp/templates'
    // doc: `https://${name}.vercel.app`,
  },
  repobeats: 'b6094745c34ee025ae4eeb67dc3c6406245ab2e5',
  images: { logo: 'images/logo.png' },
  sections: {
    projectDescription: 'Templates using [cargo generate](https://cargo-generate.github.io/)',
    features: `
- Rslib monorepo member`,
    about: 'Monorepo of templates',
    installation: 'See individual packages',
    acknowledgments: '',
    gettingStarted: 'See installation',
    roadmap: '',
    usage: '',
  },
  template: {
    bugReport: 'bug-report.yml',
    featRequest: 'feature-request.yml',
  },
  backToTop: false,
} as const;
export default config;
