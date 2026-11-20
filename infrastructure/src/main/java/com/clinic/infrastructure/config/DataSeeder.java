package com.clinic.infrastructure.config;

import com.clinic.domain.model.enums.*;
import com.clinic.infrastructure.adapter.out.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.superadmin.email:}")
    private String superAdminEmail;

    @Value("${app.superadmin.password:}")
    private String superAdminPassword;

    @Bean
    CommandLineRunner initDatabase(UserJpaRepository userRepo,
                                   OrganizationJpaRepository orgRepo,
                                   ClinicJpaRepository clinicRepo,
                                   SpecialtyJpaRepository specialtyRepo,
                                   DoctorProfileJpaRepository doctorRepo,
                                   DoctorClinicAssignmentJpaRepository assignmentRepo,
                                   PasswordEncoder encoder,
                                   Environment env) {
        return args -> {
            if (userRepo.count() == 0) {
                if (superAdminEmail != null && !superAdminEmail.isBlank()
                        && superAdminPassword != null && !superAdminPassword.isBlank()) {
                    UserEntity superAdmin = new UserEntity();
                    superAdmin.setUsername(superAdminEmail.trim());
                    superAdmin.setPassword(encoder.encode(superAdminPassword));
                    superAdmin.setRole(UserRole.SUPER_ADMIN);
                    superAdmin.setFullName("مدير المنصة");
                    superAdmin.setActive(true);
                    userRepo.save(superAdmin);
                    log.info("Initialized initial SUPER_ADMIN account for {}", superAdminEmail.trim());
                } else {
                    log.warn("Users table is empty and APP_SUPERADMIN_EMAIL / APP_SUPERADMIN_PASSWORD are not configured. Skipping SuperAdmin creation.");
                }
            }

            boolean isDevProfile = env.acceptsProfiles(Profiles.of("dev"));
            if (!seedEnabled && !isDevProfile) {
                return;
            }

            if (orgRepo.count() > 0) {
                return;
            }

            log.info("Seeding development demo organizations and sample users...");

            OrganizationEntity soloOrg = new OrganizationEntity();
            soloOrg.setName("عيادات د. محمد أحمد");
            soloOrg.setSlug("dr-mohamed-clinics");
            soloOrg.setType(OrganizationType.SOLO_DOCTOR);
            soloOrg.setPlan(SubscriptionPlan.PRO);
            soloOrg.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            soloOrg.setActive(true);
            soloOrg.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1));
            soloOrg = orgRepo.save(soloOrg);

            UserEntity doc1 = new UserEntity();
            doc1.setUsername("doctor1");
            doc1.setPassword(encoder.encode("123456"));
            doc1.setRole(UserRole.DOCTOR);
            doc1.setOwner(true);
            doc1.setOrganizationId(soloOrg.getId());
            doc1.setFullName("د. محمد أحمد");
            doc1 = userRepo.save(doc1);

            UserEntity rec1 = new UserEntity();
            rec1.setUsername("reception1");
            rec1.setPassword(encoder.encode("123456"));
            rec1.setRole(UserRole.RECEPTIONIST);
            rec1.setOrganizationId(soloOrg.getId());
            rec1.setFullName("سارة السكرتيرة");
            userRepo.save(rec1);

            SpecialtyEntity specSolo = new SpecialtyEntity();
            specSolo.setOrganizationId(soloOrg.getId());
            specSolo.setName("باطنة وقلب");
            specSolo = specialtyRepo.save(specSolo);

            DoctorProfileEntity dp1 = new DoctorProfileEntity();
            dp1.setUserId(doc1.getId());
            dp1.setOrganizationId(soloOrg.getId());
            dp1.setSpecialtyId(specSolo.getId());
            dp1.setSpecialtyName(specSolo.getName());
            dp1.setFullName(doc1.getFullName());
            dp1.setConsultationFee(BigDecimal.valueOf(250));
            doctorRepo.save(dp1);

            ClinicEntity b1 = new ClinicEntity();
            b1.setOrganizationId(soloOrg.getId());
            b1.setName("فرع شبين الكوم");
            b1.setCity("شبين الكوم");
            b1.setAddress("شارع الجمهورية - برج النيل");
            b1.setWorkingDays("SATURDAY,MONDAY,WEDNESDAY,SUNDAY,TUESDAY,THURSDAY");
            b1 = clinicRepo.save(b1);

            ClinicEntity b2 = new ClinicEntity();
            b2.setOrganizationId(soloOrg.getId());
            b2.setName("فرع الباجور");
            b2.setCity("الباجور");
            b2.setAddress("شارع المستشفى");
            b2.setWorkingDays("SUNDAY,TUESDAY,THURSDAY");
            b2 = clinicRepo.save(b2);

            for (ClinicEntity branch : new ClinicEntity[]{b1, b2}) {
                DoctorClinicAssignmentEntity a = new DoctorClinicAssignmentEntity();
                a.setOrganizationId(soloOrg.getId());
                a.setClinicId(branch.getId());
                a.setDoctorId(doc1.getId());
                a.setWorkingDays(branch.getWorkingDays());
                assignmentRepo.save(a);
            }

            OrganizationEntity center = new OrganizationEntity();
            center.setName("مركز الشفاء الطبي التخصصي");
            center.setSlug("shifa-center");
            center.setType(OrganizationType.MEDICAL_CENTER);
            center.setPlan(SubscriptionPlan.ENTERPRISE);
            center.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            center.setActive(true);
            center.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1));
            center = orgRepo.save(center);

            UserEntity centerAdmin = new UserEntity();
            centerAdmin.setUsername("center_admin");
            centerAdmin.setPassword(encoder.encode("123456"));
            centerAdmin.setRole(UserRole.ORG_ADMIN);
            centerAdmin.setOwner(true);
            centerAdmin.setOrganizationId(center.getId());
            centerAdmin.setFullName("إدارة مركز الشفاء");
            userRepo.save(centerAdmin);

            UserEntity centerRec = new UserEntity();
            centerRec.setUsername("center_rec");
            centerRec.setPassword(encoder.encode("123456"));
            centerRec.setRole(UserRole.RECEPTIONIST);
            centerRec.setOrganizationId(center.getId());
            centerRec.setFullName("استقبال مركز الشفاء");
            userRepo.save(centerRec);

            SpecialtyEntity spBatna = new SpecialtyEntity();
            spBatna.setOrganizationId(center.getId());
            spBatna.setName("باطنة");
            spBatna = specialtyRepo.save(spBatna);

            SpecialtyEntity spAtfal = new SpecialtyEntity();
            spAtfal.setOrganizationId(center.getId());
            spAtfal.setName("أطفال");
            spAtfal = specialtyRepo.save(spAtfal);

            UserEntity docBatna = new UserEntity();
            docBatna.setUsername("doc_batna");
            docBatna.setPassword(encoder.encode("123456"));
            docBatna.setRole(UserRole.DOCTOR);
            docBatna.setOrganizationId(center.getId());
            docBatna.setFullName("د. أحمد علي (باطنة)");
            docBatna = userRepo.save(docBatna);

            DoctorProfileEntity dpBatna = new DoctorProfileEntity();
            dpBatna.setUserId(docBatna.getId());
            dpBatna.setOrganizationId(center.getId());
            dpBatna.setSpecialtyId(spBatna.getId());
            dpBatna.setSpecialtyName(spBatna.getName());
            dpBatna.setFullName(docBatna.getFullName());
            doctorRepo.save(dpBatna);

            UserEntity docAtfal = new UserEntity();
            docAtfal.setUsername("doc_atfal");
            docAtfal.setPassword(encoder.encode("123456"));
            docAtfal.setRole(UserRole.DOCTOR);
            docAtfal.setOrganizationId(center.getId());
            docAtfal.setFullName("د. منى حسن (أطفال)");
            docAtfal = userRepo.save(docAtfal);

            DoctorProfileEntity dpAtfal = new DoctorProfileEntity();
            dpAtfal.setUserId(docAtfal.getId());
            dpAtfal.setOrganizationId(center.getId());
            dpAtfal.setSpecialtyId(spAtfal.getId());
            dpAtfal.setSpecialtyName(spAtfal.getName());
            dpAtfal.setFullName(docAtfal.getFullName());
            doctorRepo.save(dpAtfal);

            ClinicEntity cMain = new ClinicEntity();
            cMain.setOrganizationId(center.getId());
            cMain.setName("مركز الشفاء - الفرع الرئيسي");
            cMain.setCity("القاهرة");
            cMain.setAddress("وسط البلد");
            cMain = clinicRepo.save(cMain);

            for (UserEntity d : new UserEntity[]{docBatna, docAtfal}) {
                DoctorClinicAssignmentEntity assign = new DoctorClinicAssignmentEntity();
                assign.setOrganizationId(center.getId());
                assign.setClinicId(cMain.getId());
                assign.setDoctorId(d.getId());
                assign.setWorkingDays(cMain.getWorkingDays());
                assignmentRepo.save(assign);
            }

            UserEntity patient = new UserEntity();
            patient.setUsername("patient1");
            patient.setPassword(encoder.encode("123456"));
            patient.setRole(UserRole.PATIENT);
            patient.setFullName("كريم محمود");
            patient.setPhoneNumber("01000000000");
            userRepo.save(patient);
        };
    }
}