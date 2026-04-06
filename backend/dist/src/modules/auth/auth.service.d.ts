import { OnModuleInit } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
import { UserDocument } from '../../schemas/user.schema';
import { PhoneOtpDocument } from '../../schemas/phone-otp.schema';
import { EmailOtpDocument } from '../../schemas/email-otp.schema';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
export declare class AuthService implements OnModuleInit {
    private userModel;
    private phoneOtpModel;
    private emailOtpModel;
    private jwtService;
    private configService;
    private readonly logger;
    constructor(userModel: Model<UserDocument>, phoneOtpModel: Model<PhoneOtpDocument>, emailOtpModel: Model<EmailOtpDocument>, jwtService: JwtService, configService: ConfigService);
    onModuleInit(): Promise<void>;
    register(dto: RegisterDto): Promise<{
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
    googleVerifyIdToken(idToken: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    googleSignup(idToken: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    private verifyGoogleToken;
    verifyFirebasePhoneToken(idToken: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    sendPhoneOtp(phone: string): Promise<{
        message: string;
        devOtp?: string;
    }>;
    verifyPhoneOtp(phone: string, otp: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    private sendSmsOtp;
    sendEmailLoginOtp(email: string): Promise<{
        message: string;
        devOtp?: string;
    }>;
    verifyEmailLoginOtp(email: string, otp: string): Promise<{
        accessToken: string;
        refreshToken: string;
        user: any;
    }>;
    validateUser(userId: string): Promise<UserDocument | null>;
    private generateTokens;
    private sanitizeUser;
}
