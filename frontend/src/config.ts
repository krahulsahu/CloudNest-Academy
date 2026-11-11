// ===== Edit these values for your academy =====
export const SITE = {
  name: 'CloudNest Academy',
  trainer: 'Amar Kumar',
  whatsapp: {
    /** International format, digits only. Replace with your WhatsApp Business number. */
    phone: '919155331200',
    agentName: 'Amar Kumar',
    status: 'Typically replies within minutes',
    greeting: 'Thank you for contacting CloudNest Academy! Please let us know how we can help you.',
    /** Auto-open the chat once per session after N seconds (0 = never). */
    autoOpenSeconds: 12
  }
};

/**
 * Homepage Banner Slider Images
 * Add or replace image URLs here. They automatically slide left-to-right on the home page.
 * You can use web URLs or local paths like '/banners/slide1.jpg' placed in frontend/public/banners/
 */
export const BANNER_SLIDES = [
  {
    image: '/banners/banner1.jpg',
    title: 'Next-Gen AI & Advanced Technical Training',
    subtitle: 'High Quality Training ... Master Cloud, AI & Next-Gen Systems',
    ctaText: 'Enroll Now',
    ctaLink: '/courses'
  },
  {
    image: '/banners/banner2.jpg',
    title: 'Learn By Doing — Interactive Hands-On Labs',
    subtitle: 'Visual Explanations with Real-World Demos & Verifiable Certificates',
    ctaText: 'Explore Courses',
    ctaLink: '/courses'
  },
  {
    image: '/banners/banner3.jpg',
    title: 'Full-Stack Cloud & Software Engineering',
    subtitle: 'Master Java, Spring Boot, React, Python & Microservices Architecture',
    ctaText: 'View Curriculum',
    ctaLink: '/courses'
  },
  {
    image: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1600&q=80',
    title: 'Multi-Cloud Architecture & Infrastructure',
    subtitle: 'Enterprise AWS, Azure & Cloud Security Engineering',
    ctaText: 'Browse Cloud Paths',
    ctaLink: '/courses'
  },
  {
    image: 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1600&q=80',
    title: 'Cyber Security & Identity Governance (IAM)',
    subtitle: 'Enterprise SailPoint IdentityIQ & Zero-Trust Security Solutions',
    ctaText: 'Start Learning',
    ctaLink: '/courses'
  },
  {
    image: 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1600&q=80',
    title: 'Enterprise DevOps, Docker & Kubernetes',
    subtitle: 'Build Scalable Automated CI/CD Pipelines & Resilient Systems',
    ctaText: 'Get Certified',
    ctaLink: '/courses'
  }
];
