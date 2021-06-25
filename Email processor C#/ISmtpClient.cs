namespace LED.Managers
{
    using System;
    using System.Collections.Generic;
    using System.Net.Mail;
    using System.Text;
    using System.Threading.Tasks;

    public interface ISmtpClient
    {
        Task SendMailAsync(MailMessage message);

        void Dispose();
    }
}
