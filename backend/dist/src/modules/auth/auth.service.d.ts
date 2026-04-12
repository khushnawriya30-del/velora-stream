import { OnModuleInit } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
import { UserDocument, AuthProvider } from '../../schemas/user.schema';
import { PhoneOtpDocument } from '../../schemas/phone-otp.schema';
import { EmailOtpDocument } from '../../schemas/email-otp.schema';
import { TvQrTokenDocument } from '../../schemas/tv-qr-token.schema';
import { ReferralService } from '../referral/referral.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
export declare class AuthService implements OnModuleInit {
    private userModel;
    private phoneOtpModel;
    private emailOtpModel;
    private tvQrTokenModel;
    private jwtService;
    private configService;
    private readonly referralService;
    private readonly logger;
    constructor(userModel: Model<UserDocument>, phoneOtpModel: Model<PhoneOtpDocument>, emailOtpModel: Model<EmailOtpDocument>, tvQrTokenModel: Model<TvQrTokenDocument>, jwtService: JwtService, configService: ConfigService, referralService: ReferralService);
    onModuleInit(): Promise<void>;
    register(dto: RegisterDto, ipAddress?: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    login(dto: LoginDto): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    googleLogin(profile: any): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    refreshToken(refreshToken: string): Promise<{
        accessToken: string;
        refreshToken: string;
    }>;
    forgotPassword(email: string): Promise<{
        message: string;
    }>;
    verifyOtp(email: string, otp: string): Promise<{
        message: string;
        resetToken: string;
    }>;
    changePassword(userId: string, currentPassword: string, newPassword: string): Promise<{
        message: string;
    }>;
    resetPassword(token: string, newPassword: string): Promise<{
        message: string;
    }>;
    googleVerifyIdToken(idToken: string, referralCode?: string, ipAddress?: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    googleSignup(idToken: string, referralCode?: string, ipAddress?: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    googleVerifyAccessToken(accessToken: string, referralCode?: string, ipAddress?: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    private verifyGoogleToken;
    verifyFirebasePhoneToken(idToken: string, referralCode?: string, ipAddress?: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    sendPhoneOtp(phone: string): Promise<{
        message: string;
        devOtp?: string;
    }>;
    verifyPhoneOtp(phone: string, otp: string, referralCode?: string, ipAddress?: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    private sendSmsOtp;
    sendEmailLoginOtp(email: string): Promise<{
        message: string;
        devOtp?: string;
    }>;
    verifyEmailLoginOtp(email: string, otp: string, referralCode?: string, ipAddress?: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    generateTvQrToken(): Promise<{
        token: string;
        expiresAt: Date;
    }>;
    checkTvQrToken(token: string): Promise<{
        status: string;
        accessToken?: undefined;
        refreshToken?: undefined;
        user?: undefined;
    } | {
        status: string;
        accessToken: string;
        refreshToken: string;
        user: {
            isPremium: boolean;
            premiumPlan: string;
            premiumExpiresAt: Date;
            id: import("mongoose").Types.ObjectId;
            name: string;
            email: string;
            avatarUrl: string;
            role: import("../../schemas/user.schema").UserRole;
            authProvider: AuthProvider;
            isEmailVerified: boolean;
        };
    }>;
    approveTvQrToken(token: string, userId: string): Promise<{
        message: string;
    }>;
    validateUser(userId: string): Promise<UserDocument | null>;
    private generateTokens;
    private sanitizeUser;
}
