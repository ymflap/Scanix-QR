package com.scanix.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "سياسة الخصوصية",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "آخر تحديث: 17 أكتوبر 2026",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PrivacySection(
            title = "1. مقدمة",
            body = "توضح هذه السياسة كيفية تعامل تطبيق Scanix QR " +
                "مع المعلومات عند استخدام ميزات مسح رموز QR والباركود."
        )

        PrivacySection(
            title = "2. الكاميرا والصور",
            body = "يطلب التطبيق إذن الكاميرا لتشغيل المسح المباشر. " +
                "يمكنك أيضًا اختيار صورة من جهازك لفحص الرمز الموجود فيها. " +
                "تُستخدم هذه الصور لغرض قراءة الرمز داخل التطبيق."
        )

        PrivacySection(
            title = "3. نتائج المسح وسجل العمليات",
            body = "يمكن حفظ نتائج المسح في سجل محلي على جهازك إذا كانت " +
                "ميزة حفظ النتائج مفعّلة. لا يحتاج التطبيق إلى إنشاء حساب. " +
                "يمكنك إيقاف حفظ النتائج من الإعدادات. وقد تبقى النتائج " +
                "المحفوظة سابقًا حتى تحذفها من سجل التطبيق."
        )

        PrivacySection(
            title = "4. مشاركة النتائج وفتح الروابط",
            body = "عندما تختار نسخ نتيجة أو مشاركتها أو فتح رابط، " +
                "يستخدم التطبيق الوظيفة التي اخترتها. وقد تنتقل المعلومات " +
                "إلى التطبيق الآخر أو الموقع الذي اخترت فتحه، وفقًا " +
                "لإجراءك وسياسات ذلك الطرف."
        )

        PrivacySection(
            title = "5. جمع البيانات وإرسالها",
            body = "وفقًا للتنفيذ الحالي للتطبيق، لا يتضمن التطبيق خادمًا " +
                "خاصًا لإرسال نتائج المسح إليه، ولا يطلب إنشاء حساب. " +
                "قد تخضع بيانات التشخيص أو التشغيل التي تتعامل معها " +
                "خدمات Android أو خدمات Google المستخدمة في معالجة الرموز " +
                "لسياسات تلك الخدمات."
        )

        PrivacySection(
            title = "6. أمان البيانات",
            body = "تُحفظ نتائج السجل محليًا على الجهاز. ننصحك بحماية " +
                "جهازك وحذف النتائج التي لا ترغب في الاحتفاظ بها. " +
                "لا توجد وسيلة إلكترونية تضمن الأمان المطلق."
        )

        PrivacySection(
            title = "7. خصوصية الأطفال",
            body = "التطبيق أداة لمسح الرموز، وليس مصممًا لجمع معلومات " +
                "شخصية من الأطفال."
        )

        PrivacySection(
            title = "8. التغييرات على السياسة",
            body = "قد يتم تحديث هذه السياسة عند إضافة ميزات جديدة أو " +
                "تغيير طريقة عمل التطبيق. ستُحدّث خانة آخر تحديث عند إجراء " +
                "تغييرات جوهرية."
        )

        PrivacySection(
            title = "9. التواصل",
            body = "للاستفسارات المتعلقة بالخصوصية أو التطبيق، استخدم " +
                "صفحة المستودع الرسمية للمشروع على GitHub:\n" +
                "https://github.com/ymflap/Scanix-QR"
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PrivacySection(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
