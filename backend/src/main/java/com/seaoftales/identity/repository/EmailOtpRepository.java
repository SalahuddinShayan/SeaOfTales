package com.seaoftales.identity.repository;
import com.seaoftales.identity.entity.EmailOtp; import com.seaoftales.identity.enums.OtpPurpose; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface EmailOtpRepository extends JpaRepository<EmailOtp,Long>{Optional<EmailOtp> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email,OtpPurpose purpose);}
