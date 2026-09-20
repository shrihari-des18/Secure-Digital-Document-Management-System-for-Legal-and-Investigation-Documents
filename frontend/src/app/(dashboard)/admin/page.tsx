'use client';

import { useState } from 'react';
import {
  Plus,
  Search,
  Lock,
  Key,
  Database,
  Building,
  Mail,
  BadgeAlert,
  UserCheck,
  Shield,
} from 'lucide-react';
import { clsx } from 'clsx';
import { usersApi } from '@/lib/api/users';
import { ROLE_LABELS, ROLE_COLORS } from '@/stores/auth-store';
import { Modal } from '@/components/ui/modal';
import type { CreateUserRequest } from '@/types';
import { RoleEnum } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export default function AdminPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRole, setSelectedRole] = useState<RoleEnum | 'ALL'>('ALL');

  // Register modal state
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [badgeNumber, setBadgeNumber] = useState('');
  const [department, setDepartment] = useState('');
  const [role, setRole] = useState<RoleEnum>(RoleEnum.INVESTIGATING_OFFICER);
  const [formError, setFormError] = useState<string | null>(null);

  // Fetch users
  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getAll(),
  });

  // Create user mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateUserRequest) => usersApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setIsRegisterOpen(false);
      setUsername('');
      setPassword('');
      setFullName('');
      setEmail('');
      setBadgeNumber('');
      setDepartment('');
      setFormError(null);
    },
    onError: (err: any) => {
      setFormError(err.message || 'User creation failed');
    },
  });

  const handleRegisterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      username: username.trim(),
      password,
      fullName: fullName.trim(),
      email: email.trim(),
      badgeNumber: badgeNumber.trim() || undefined,
      department: department.trim() || undefined,
      role,
    });
  };

  const filteredUsers = users.filter((u) => {
    const q = searchQuery.toLowerCase();
    const matchSearch =
      u.fullName.toLowerCase().includes(q) ||
      u.username.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q) ||
      (u.badgeNumber && u.badgeNumber.toLowerCase().includes(q)) ||
      (u.department && u.department.toLowerCase().includes(q));

    const matchRole = selectedRole === 'ALL' || u.role === selectedRole;
    return matchSearch && matchRole;
  });

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">System Actors & RBAC Directory</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Role-based and attribute-scoped access control authority, officer registries, and security posture
          </p>
        </div>

        <button
          onClick={() => setIsRegisterOpen(true)}
          className="btn-primary shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>Onboard New Actor</span>
        </button>
      </div>

      {/* Security Engine Specs Grid */}
      <details className="group mt-6">
        <summary className="text-xs font-mono font-semibold text-zinc-400 uppercase tracking-wider cursor-pointer hover:text-zinc-200 transition-colors inline-flex items-center gap-2">
          <span>View Security Posture & Cryptographic Specs</span>
          <span className="group-open:rotate-180 transition-transform">▼</span>
        </summary>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 pt-4">
          <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 flex items-start gap-4">
            <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300 shrink-0">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[11px] text-zinc-500 uppercase font-semibold font-mono block">Authentication</span>
              <span className="text-sm font-bold text-zinc-200 mt-1 block">HMAC-SHA256 JWT Bearer</span>
            </div>
          </div>

          <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 flex items-start gap-4">
            <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-amber-400 shrink-0">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[11px] text-zinc-500 uppercase font-semibold font-mono block">Access Control</span>
              <span className="text-sm font-bold text-zinc-200 mt-1 block">RBAC + Fine-Grained ABAC</span>
            </div>
          </div>

          <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 flex items-start gap-4">
            <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-emerald-400 shrink-0">
              <Key className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[11px] text-zinc-500 uppercase font-semibold font-mono block">Crypto Authority</span>
              <span className="text-sm font-bold text-zinc-200 mt-1 block">BouncyCastle ECDSA-P256</span>
            </div>
          </div>

          <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 flex items-start gap-4">
            <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-blue-400 shrink-0">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[11px] text-zinc-500 uppercase font-semibold font-mono block">Ledger Commit Gate</span>
              <span className="text-sm font-bold text-zinc-200 mt-1 block">Append-Only Hash Chain</span>
            </div>
          </div>
        </div>
      </details>

      {/* Filter and Search Bar */}
      <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl space-y-4">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="relative w-full md:w-96">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search officer name, username, email, badge..."
              className="input-field pl-9"
            />
          </div>
        </div>

        {/* Role Pills */}
        <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-zinc-800/80 font-mono text-xs">
          <span className="text-[11px] font-semibold text-zinc-500 uppercase">Role:</span>
          {(['ALL', ...Object.values(RoleEnum)] as const).map((r) => (
            <button
              key={r}
              onClick={() => setSelectedRole(r)}
              className={clsx(
                'text-[11px] px-2.5 py-1 rounded-lg font-medium transition-colors',
                selectedRole === r
                  ? 'bg-zinc-100 text-zinc-950 font-bold border border-zinc-100'
                  : 'bg-zinc-900/60 text-zinc-400 hover:text-zinc-200 border border-zinc-800'
              )}
            >
              {r === 'ALL' ? 'All Roles' : ROLE_LABELS[r] || r}
            </button>
          ))}
        </div>
      </div>

      {/* Users Grid */}
      {isLoading ? (
        <div className="py-20 text-center text-xs text-zinc-500">Loading user directory...</div>
      ) : filteredUsers.length === 0 ? (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center text-xs text-zinc-500">
          No system users found matching your criteria.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredUsers.map((u) => {
            const roleColor = ROLE_COLORS[u.role] || 'bg-zinc-900 text-zinc-300 border-zinc-800';

            return (
              <div
                key={u.id}
                className="border border-zinc-800 bg-zinc-950 rounded-xl p-5 flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-200 font-bold text-sm font-mono">
                        {u.fullName.slice(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <h3 className="text-sm font-bold tracking-tight text-zinc-100">{u.fullName}</h3>
                        <p className="text-[11px] text-zinc-500 font-mono">@{u.username}</p>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2 my-3 text-xs">
                    <div className="flex items-center gap-1.5 text-zinc-400">
                      <Mail className="w-3.5 h-3.5 shrink-0 text-zinc-500" />
                      <span className="truncate">{u.email}</span>
                    </div>

                    <div className="flex items-center gap-1.5 text-zinc-400">
                      <Building className="w-3.5 h-3.5 shrink-0 text-zinc-500" />
                      <span className="truncate">{u.department || 'General Division'}</span>
                    </div>

                    {u.badgeNumber && (
                      <div className="flex items-center gap-1.5 text-zinc-400">
                        <BadgeAlert className="w-3.5 h-3.5 shrink-0 text-amber-400" />
                        <span className="font-mono text-zinc-200 font-medium">Badge: {u.badgeNumber}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-zinc-800 flex items-center justify-between">
                  <span className={clsx('text-[10px] font-mono px-2.5 py-0.5 rounded font-bold border', roleColor)}>
                    {ROLE_LABELS[u.role] || u.role}
                  </span>
                  <span className="text-[10px] font-mono text-zinc-500">
                    ID: {u.id.slice(0, 8)}...
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Onboard User Modal */}
      <Modal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        title="Onboard New System Actor"
        subtitle="Register officer, analyst, prosecutor, or court personnel with RBAC role"
        maxWidth="lg"
      >
        <form onSubmit={handleRegisterSubmit} className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Username <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. officer_vikram"
                required
                className="input-field font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Initial Password <span className="text-red-400">*</span>
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Password@123"
                required
                className="input-field"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Full Official Name <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Inspector Vikram Rathore"
                required
                className="input-field"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Official Email <span className="text-red-400">*</span>
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="vikram@police.gov.in"
                required
                className="input-field"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                RBAC Assigned Role <span className="text-red-400">*</span>
              </label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as RoleEnum)}
                className="select-field"
              >
                {Object.values(RoleEnum).map((r) => (
                  <option key={r} value={r} className="bg-zinc-900 text-zinc-100">
                    {ROLE_LABELS[r] || r}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">Badge Number / ID</label>
              <input
                type="text"
                value={badgeNumber}
                onChange={(e) => setBadgeNumber(e.target.value)}
                placeholder="DL-4509"
                className="input-field font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">Department / Unit</label>
              <input
                type="text"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                placeholder="Cyber Crime Cell"
                className="input-field"
              />
            </div>
          </div>

          {formError && (
            <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
              {formError}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsRegisterOpen(false)}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="btn-primary"
            >
              <UserCheck className="w-3.5 h-3.5" />
              <span>{createMutation.isPending ? 'Registering...' : 'Register System User'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
