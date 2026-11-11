export interface User {
  id: number; name: string; email: string; role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';
  emailVerified: boolean; country?: string; headline?: string; bio?: string; hasPassword: boolean;
}
export interface AuthResponse { accessToken: string; refreshToken: string; user: User }
export interface MessageResponse { message: string; devCode?: string | null }

export interface CourseCard {
  id: number; slug: string; title: string; subtitle?: string; kind: 'COURSE' | 'BUNDLE'; category: string; level: string;
  priceInr: number; priceUsd: number; compareAtInr?: number | null; featured: boolean; isNew: boolean; badges?: string;
  theme?: string | null; thumbnailUrl?: string | null; instructorName?: string | null; itemCount: number; lessonCount: number;
  totalSeconds: number; rating: number; reviewCount: number; status: string;
}
export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface LessonSummary { id: number; title: string; type: 'VIDEO' | 'TEXT' | 'QUIZ'; durationSeconds: number; preview: boolean; completed: boolean }
export interface Section { id: number; title: string; lessons: LessonSummary[] }
export interface Review { id: number; userName: string; rating: number; comment?: string; createdAt: string; courseTitle: string; visible: boolean }
export interface CourseDetail {
  card: CourseCard; description?: string; objectives: string[]; prerequisites: string[]; audience?: string;
  previewVideoUrl?: string | null; instructor?: { id: number; name: string; headline?: string; bio?: string } | null;
  sections: Section[]; bundleItems: CourseCard[]; inBundles: CourseCard[]; related: CourseCard[]; hasAccess: boolean;
  reviews: Review[]; canReview: boolean; seoTitle?: string; seoDescription?: string;
}
export interface Category { id: number; name: string }
export interface Plan { id?: number; code: string; name: string; description?: string; priceInr: number; priceUsd: number; periodDays: number; category?: string | null; level?: string | null; active: boolean }
export interface PublicConfig { razorpayKeyId?: string | null; razorpay: boolean; stripe: boolean; mockPayments: boolean; refundDays: number }

export interface Question { id: number; prompt: string; type: 'SINGLE' | 'MULTI' | 'TRUE_FALSE' | 'FILL'; options: string[] }
export interface Lesson {
  id: number; title: string; type: 'VIDEO' | 'TEXT' | 'QUIZ'; durationSeconds: number; sectionTitle: string;
  videoUrl?: string | null; captionsUrl?: string | null; content?: string | null; resources: { label: string; url: string }[];
  questions: Question[]; passPercent?: number; maxAttempts?: number | null; timeLimitMinutes?: number | null;
  attemptsUsed: number; bestPercent?: number | null; completed: boolean; positionSeconds: number; bookmarked: boolean;
  note?: string | null; hasAccess: boolean; prevId?: number | null; nextId?: number | null;
}
export interface LearnCourse { card: CourseCard; sections: Section[]; completed: number; total: number; resumeLessonId?: number | null; certificateCode?: string | null; hasAccess: boolean }
export interface QuizResult { score: number; total: number; percent: number; passed: boolean; results: { id: number; correct: boolean; correctAnswer: string; explanation?: string }[] }
export interface MyCourse { card: CourseCard; completed: number; total: number; resumeLessonId?: number | null; certificateCode?: string | null; access: string; expiresAt?: string | null }
export interface Certificate { code: string; studentName: string; courseTitle: string; courseSlug: string; issuedAt: string }
export interface Order {
  id: number; invoiceNumber?: string | null; item: string; currency: string; listPrice: number; credit: number; discount: number;
  tax: number; total: number; status: string; provider?: string; createdAt: string; paidAt?: string | null; userEmail: string; couponCode?: string | null;
}
export interface Subscription { id: number; planName: string; planCode: string; startsAt: string; expiresAt: string; autoRenew: boolean; status: string }
export interface Notification { id: number; title: string; body: string; link?: string; seen: boolean; createdAt: string }
export interface Bookmark { lessonId: number; lessonTitle: string; courseSlug: string; courseTitle: string }
export interface QuizHistory { courseTitle: string; lessonTitle: string; score: number; total: number; passed: boolean; at: string }
export interface Ticket { id: number; type: string; name: string; email: string; subject?: string; message: string; reply?: string | null; status: string; createdAt: string }

export interface Quote { currency: string; listPrice: number; credit: number; discount: number; total: number; itemName: string; couponMessage?: string | null }
export interface CheckoutResponse {
  orderId: number; provider: 'FREE' | 'RAZORPAY' | 'STRIPE' | 'MOCK'; total: number; currency: string; completed: boolean;
  razorpayOrderId?: string; razorpayKeyId?: string; stripeUrl?: string; userName: string; userEmail: string; description: string; redirect: string;
}

export interface QuestionEdit { id?: number | null; prompt: string; type: string; options: string; correct: string; explanation: string }
export interface LessonEdit {
  id?: number | null; title: string; type: string; durationSeconds: number; videoUrl?: string; captionsUrl?: string; content?: string;
  resources?: string; preview: boolean; passPercent?: number | null; maxAttempts?: number | null; timeLimitMinutes?: number | null;
  randomize: boolean; questions: QuestionEdit[]; _key?: string;
}
export interface SectionEdit { id?: number | null; title: string; lessons: LessonEdit[]; _key?: string }
export interface CourseEdit {
  id: number; slug: string; title: string; subtitle?: string; description?: string; category?: string; level?: string; kind: string;
  priceInr: number; priceUsd: number; compareAtInr?: number | null; badges?: string; theme?: string | null; thumbnailUrl?: string;
  previewVideoUrl?: string; objectives?: string; prerequisites?: string; audience?: string; seoTitle?: string; seoDescription?: string;
  status: string; scheduledAt?: string | null; featured: boolean; isNew: boolean; bundleItemIds: number[]; sections: SectionEdit[]; instructorName?: string;
}
export interface InstructorCourse { card: CourseCard; students: number; revenueInr: number; revenueUsd: number; certificates: number }
export interface Student { userId: number; name: string; email: string; enrolledAt: string; source: string; completed: number; total: number; lastQuizPercent?: number | null }

export interface AdminStats {
  students: number; instructors: number; courses: number; publishedCourses: number; pendingCourses: number; enrollments: number;
  activeLearners30d: number; certificates: number; activeSubscriptions: number; revenueInr: number; revenueUsd: number;
  subscriptionRevenueInr: number; subscriptionRevenueUsd: number; paidOrders: number; refundedOrders: number;
  monthly: { month: string; revenueInr: number; revenueUsd: number; orders: number }[];
  popular: { id: number; title: string; value: number }[]; profitableInr: { id: number; title: string; value: number }[]; retentionPercent: number;
}
export interface AdminUser { id: number; name: string; email: string; role: string; enabled: boolean; emailVerified: boolean; createdAt: string }
export interface Coupon { id?: number | null; code: string; percentOff: number; active: boolean; expiresAt?: string | null; maxUses?: number | null; uses: number; courseSlug?: string | null }
