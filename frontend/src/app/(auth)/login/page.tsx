'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { LogIn, Eye, EyeOff, Loader2 } from 'lucide-react';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/stores/auth-store';
import { ApiError } from '@/lib/api-client';

const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: 'officer_sharma',
      password: 'Password123!',
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setError(null);
    try {
      const response = await authApi.login(data);
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
      <h2 className="text-xl font-bold tracking-tight text-zinc-100 mb-1">Sign In</h2>
      <p className="text-xs text-zinc-400 mb-6">
        Enter your credentials to access the digital evidence vault
      </p>

      {error && (
        <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs animate-fade-in">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Username */}
        <div>
          <label htmlFor="login-username" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
            Username
          </label>
          <input
            id="login-username"
            type="text"
            autoComplete="username"
            className="input-field"
            placeholder="e.g. io_sharma"
            {...register('username')}
          />
          {errors.username && (
            <p className="text-xs text-red-400 mt-1">{errors.username.message}</p>
          )}
        </div>

        {/* Password */}
        <div>
          <label htmlFor="login-password" className="block text-xs font-semibold text-zinc-300 mb-1.5 uppercase tracking-wider font-mono">
            Password
          </label>
          <div className="relative">
            <input
              id="login-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              className="input-field pr-10"
              placeholder="••••••••"
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

        {/* Submit */}
        <button
          type="submit"
          disabled={isSubmitting}
          className="btn-primary w-full mt-3"
        >
          {isSubmitting ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <LogIn className="w-4 h-4" />
          )}
          <span>{isSubmitting ? 'Authenticating...' : 'Sign In'}</span>
        </button>
      </form>

      <p className="text-center text-xs text-zinc-400 mt-6">
        Need an officer account?{' '}
        <Link href="/register" className="text-zinc-100 hover:underline font-semibold">
          Register here
        </Link>
      </p>
    </div>
  );
}
