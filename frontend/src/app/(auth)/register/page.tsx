'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { UserPlus, Eye, EyeOff, Loader2, ChevronDown } from 'lucide-react';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/stores/auth-store';
import { ROLE_LABELS } from '@/stores/auth-store';
import { RoleEnum } from '@/types';
import { ApiError } from '@/lib/api-client';

const registerSchema = z.object({
  username: z
    .string()
    .min(3, 'Username must be at least 3 characters')
    .max(64, 'Username cannot exceed 64 characters'),
  password: z
    .string()
    .min(6, 'Password must be at least 6 characters')
    .max(100, 'Password cannot exceed 100 characters'),
  fullName: z
    .string()
    .min(1, 'Full name is required')
    .max(128, 'Full name cannot exceed 128 characters'),
  email: z
    .string()
    .email('Enter a valid email address')
    .max(128, 'Email cannot exceed 128 characters'),
  badgeNumber: z.string().optional(),
  department: z.string().optional(),
  role: z.nativeEnum(RoleEnum, { errorMap: () => ({ message: 'Select a role' }) }),
});

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormData) => {
    setError(null);
    try {
      const response = await authApi.register(data);
      setAuth(response.token, response.user);
      router.push('/dashboard');
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('An unexpected error occurred. Please try again.');
      }
    }
  };

  return (
    <div className="border border-zinc-800 bg-zinc-950 p-6 md:p-8 rounded-xl shadow-xl animate-fade-in">
      <h2 className="text-xl font-bold tracking-tight text-zinc-100 mb-1">Create Account</h2>
      <p className="text-xs text-zinc-400 mb-6">
        Register an official actor credential to access the evidence vault
      </p>

      {error && (
        <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs animate-fade-in">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Full Name */}
        <div>
          <label htmlFor="reg-fullname" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
            Full Name <span className="text-red-400">*</span>
          </label>
          <input
            id="reg-fullname"
            type="text"
            className="input-field"
            placeholder="Inspector Rajesh Sharma"
            {...register('fullName')}
          />
          {errors.fullName && (
            <p className="text-xs text-red-400 mt-1">{errors.fullName.message}</p>
          )}
        </div>

        {/* Username & Email row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="reg-username" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
              Username <span className="text-red-400">*</span>
            </label>
            <input
              id="reg-username"
              type="text"
              autoComplete="username"
              className="input-field"
              placeholder="rsharma"
              {...register('username')}
            />
            {errors.username && (
              <p className="text-xs text-red-400 mt-1">{errors.username.message}</p>
            )}
          </div>
          <div>
            <label htmlFor="reg-email" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
              Email <span className="text-red-400">*</span>
            </label>
            <input
              id="reg-email"
              type="email"
              autoComplete="email"
              className="input-field"
              placeholder="sharma@police.gov.in"
              {...register('email')}
            />
            {errors.email && (
              <p className="text-xs text-red-400 mt-1">{errors.email.message}</p>
            )}
          </div>
        </div>

        {/* Password */}
        <div>
          <label htmlFor="reg-password" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
            Password <span className="text-red-400">*</span>
          </label>
          <div className="relative">
            <input
              id="reg-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              className="input-field pr-10"
              placeholder="Minimum 6 characters"
              {...register('password')}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-100 transition-colors"
              tabIndex={-1}
            >
              {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="text-xs text-red-400 mt-1">{errors.password.message}</p>
          )}
        </div>

        {/* Role */}
        <div>
          <label htmlFor="reg-role" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
            Role Authorization <span className="text-red-400">*</span>
          </label>
          <div className="relative">
            <select
              id="reg-role"
              className="select-field"
              defaultValue=""
              {...register('role')}
            >
              <option value="" disabled>
                Select your statutory role
              </option>
              {Object.values(RoleEnum).map((role) => (
                <option key={role} value={role} className="bg-zinc-900 text-zinc-100">
                  {ROLE_LABELS[role]}
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400 pointer-events-none" />
          </div>
          {errors.role && (
            <p className="text-xs text-red-400 mt-1">{errors.role.message}</p>
          )}
        </div>

        {/* Badge Number & Department row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="reg-badge" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
              Badge / ID Number
            </label>
            <input
              id="reg-badge"
              type="text"
              className="input-field"
              placeholder="e.g. DL-8842"
              {...register('badgeNumber')}
            />
          </div>
          <div>
            <label htmlFor="reg-dept" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
              Department
            </label>
            <input
              id="reg-dept"
              type="text"
              className="input-field"
              placeholder="e.g. Cyber Crime Cell"
              {...register('department')}
            />
          </div>
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={isSubmitting}
          className="btn-primary w-full mt-3"
        >
          {isSubmitting ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <UserPlus className="w-4 h-4" />
          )}
          <span>{isSubmitting ? 'Registering...' : 'Register Account'}</span>
        </button>
      </form>

      <p className="text-center text-xs text-zinc-400 mt-6">
        Already registered?{' '}
        <Link href="/login" className="text-zinc-100 hover:underline font-semibold">
          Sign in
        </Link>
      </p>
    </div>
  );
}
